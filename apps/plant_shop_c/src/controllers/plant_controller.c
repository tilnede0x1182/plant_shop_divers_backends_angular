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
extern PGconn* DATABASE_CONNECTION;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Envoie une reponse JSON formatee.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param json_obj Objet JSON a envoyer
 * @param code Code HTTP de reponse
 */
static void send_json_reply(struct mg_connection* mongoose_connection, cJSON* json_obj, int http_code) {
	char *json_text = cJSON_PrintUnformatted(json_obj);
	mg_http_reply(mongoose_connection, http_code, "Content-Type: application/json\r\n", "%s", json_text);
	free(json_text);
	if (json_obj) cJSON_Delete(json_obj);
}

/**
 * Verifie si l utilisateur courant est administrateur.
 *
 * @param http_message Message HTTP contenant le cookie
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* hm) {
	int user_id = get_current_user_id(hm);
	return user_repo_is_admin(DATABASE_CONNECTION, user_id);
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Recupere une plante par son ID.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param plant_identifier ID de la plante
 */
void plant_get(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int plant_identifier) {
	Plant plant;
	if (!plant_repo_find(DATABASE_CONNECTION, plant_identifier, &plant)) { mg_http_reply(mongoose_connection, 404, "", ""); return; }
	cJSON *json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", plant.id);
	cJSON_AddStringToObject(json_obj, "name", plant.name);
	cJSON_AddStringToObject(json_obj, "description", plant.description);
	cJSON_AddNumberToObject(json_obj, "price", plant.price);
	cJSON_AddNumberToObject(json_obj, "stock", plant.stock);
	send_json_reply(mongoose_connection, json_obj, 200);
	(void)http_message;
}

/**
 * Callback pour ajouter une plante a un tableau JSON.
 *
 * @param plant Pointeur vers la plante
 * @param json_array Tableau JSON cible
 */
static void admin_plants_list_cb(Plant* plant, void* json_array) {
	cJSON *json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", plant->id);
	cJSON_AddStringToObject(json_obj, "name", plant->name);
	cJSON_AddNumberToObject(json_obj, "price", plant->price);
	cJSON_AddNumberToObject(json_obj, "stock", plant->stock);
	cJSON_AddItemToArray((cJSON*)json_array, json_obj);
}

/**
 * Liste toutes les plantes (acces public).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void plants_list_public(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	(void)http_message;
	cJSON *json_array = cJSON_CreateArray();
	plant_repo_each(DATABASE_CONNECTION, admin_plants_list_cb, json_array);
	send_json_reply(mongoose_connection, json_array, 200);
}

/**
 * Liste toutes les plantes (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
void admin_plants_list(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON *json_array = cJSON_CreateArray();
	plant_repo_each(DATABASE_CONNECTION, admin_plants_list_cb, json_array);
	send_json_reply(mongoose_connection, json_array, 200);
}

/**
 * Remplit une structure Plant a partir du JSON.
 *
 * @param plant Pointeur vers la structure a remplir
 * @param json_source Objet JSON source
 */
static void fill_plant_from_json(Plant* plant, cJSON* json_source) {
	const char *name = cJSON_GetStringValue(cJSON_GetObjectItem(json_source, "name"));
	const char *description = cJSON_GetStringValue(cJSON_GetObjectItem(json_source, "description"));
	if (name) strncpy(plant->name, name, sizeof(plant->name) - 1);
	if (description) strncpy(plant->description, description, sizeof(plant->description) - 1);
	plant->price = cJSON_GetObjectItem(json_source, "price")->valueint;
	plant->stock = cJSON_GetObjectItem(json_source, "stock")->valueint;
}

/**
 * Ajoute une nouvelle plante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees JSON
 */
void admin_plants_add(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON* json_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_data) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	Plant plant = {0};
	fill_plant_from_json(&plant, json_data);
	plant.id = plant_repo_add(DATABASE_CONNECTION, &plant);
	cJSON_Delete(json_data);
	cJSON *response_json = cJSON_CreateObject();
	cJSON_AddNumberToObject(response_json, "id", plant.id);
	send_json_reply(mongoose_connection, response_json, 201);
}

/**
 * Modifie une plante existante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees JSON
 * @param plant_identifier Identifiant de la plante a modifier
 */
void admin_plants_patch(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int plant_identifier) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON* json_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_data) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	plant_repo_patch(DATABASE_CONNECTION, plant_identifier, json_data);
	cJSON_Delete(json_data);
	mg_http_reply(mongoose_connection, 200, "", "");
}

/**
 * Supprime une plante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param plant_identifier Identifiant de la plante a supprimer
 */
void admin_plants_del(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int plant_identifier) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	plant_repo_del(DATABASE_CONNECTION, plant_identifier);
	mg_http_reply(mongoose_connection, 200, "", "");
}
