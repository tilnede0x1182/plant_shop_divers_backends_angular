/* ==============================================================================
   Importations
   ============================================================================== */
#include "plant_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/plant_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdint.h>
#include "../utils/utils.h"

/* ==============================================================================
   Données
   ============================================================================== */
extern PGconn* DB;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Envoie une reponse JSON formatee.
 *
 * @param c Connexion Mongoose
 * @param json_obj Objet JSON a envoyer
 * @param code Code HTTP de reponse
 */
static void send_json_reply(struct mg_connection* c, cJSON* json_obj, int code) {
	char *text = cJSON_PrintUnformatted(json_obj);
	mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
	free(text);
	if (json_obj) cJSON_Delete(json_obj);
}

/**
 * Verifie si l utilisateur courant est administrateur.
 *
 * @param hm Message HTTP contenant le cookie
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* hm) {
	int user_id = get_current_user_id(hm);
	return user_repo_is_admin(DB, user_id);
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Recupere une plante par son ID.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id ID de la plante
 */
void plant_get(struct mg_connection* c, struct mg_http_message *hm, int id) {
	Plant plant;
	if (!plant_repo_find(DB, id, &plant)) { mg_http_reply(c, 404, "", ""); return; }
	cJSON *json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", plant.id);
	cJSON_AddStringToObject(json_obj, "name", plant.name);
	cJSON_AddStringToObject(json_obj, "description", plant.description);
	cJSON_AddNumberToObject(json_obj, "price", plant.price);
	cJSON_AddNumberToObject(json_obj, "stock", plant.stock);
	send_json_reply(c, json_obj, 200);
	(void)hm;
}

/**
 * Callback pour ajouter une plante a un tableau JSON.
 *
 * @param plant Pointeur vers la plante
 * @param arr Tableau JSON cible
 */
static void admin_plants_list_cb(Plant* plant, void* arr) {
	cJSON *json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", plant->id);
	cJSON_AddStringToObject(json_obj, "name", plant->name);
	cJSON_AddNumberToObject(json_obj, "price", plant->price);
	cJSON_AddNumberToObject(json_obj, "stock", plant->stock);
	cJSON_AddItemToArray((cJSON*)arr, json_obj);
}

/**
 * Liste toutes les plantes (acces public).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
void plants_list_public(struct mg_connection* c, struct mg_http_message *hm) {
	cJSON *arr = cJSON_CreateArray();
	plant_repo_each(DB, admin_plants_list_cb, arr);
	send_json_reply(c, arr, 200);
}

/**
 * Liste toutes les plantes (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
void admin_plants_list(struct mg_connection* c, struct mg_http_message *hm) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON *arr = cJSON_CreateArray();
	plant_repo_each(DB, admin_plants_list_cb, arr);
	send_json_reply(c, arr, 200);
}

/**
 * Remplit une structure Plant a partir du JSON.
 *
 * @param plant Pointeur vers la structure a remplir
 * @param json Objet JSON source
 */
static void fill_plant_from_json(Plant* plant, cJSON* json) {
	const char *name = cJSON_GetStringValue(cJSON_GetObjectItem(json, "name"));
	const char *desc = cJSON_GetStringValue(cJSON_GetObjectItem(json, "description"));
	if (name) strncpy(plant->name, name, sizeof(plant->name) - 1);
	if (desc) strncpy(plant->description, desc, sizeof(plant->description) - 1);
	plant->price = cJSON_GetObjectItem(json, "price")->valueint;
	plant->stock = cJSON_GetObjectItem(json, "stock")->valueint;
}

/**
 * Ajoute une nouvelle plante (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les donnees JSON
 */
void admin_plants_add(struct mg_connection* c, struct mg_http_message *hm) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	Plant plant = {0};
	fill_plant_from_json(&plant, json);
	plant.id = plant_repo_add(DB, &plant);
	cJSON_Delete(json);
	cJSON *out = cJSON_CreateObject();
	cJSON_AddNumberToObject(out, "id", plant.id);
	send_json_reply(c, out, 201);
}

/**
 * Modifie une plante existante (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les donnees JSON
 * @param id ID de la plante a modifier
 */
void admin_plants_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	plant_repo_patch(DB, id, json);
	cJSON_Delete(json);
	mg_http_reply(c, 200, "", "");
}

/**
 * Supprime une plante (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id ID de la plante a supprimer
 */
void admin_plants_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	plant_repo_del(DB, id);
	mg_http_reply(c, 200, "", "");
}
