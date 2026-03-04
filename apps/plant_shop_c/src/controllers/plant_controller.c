#include "plant_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/plant_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdint.h>
#include "../utils/utils.h"

extern PGconn* DB;

/**
 * Envoie une réponse JSON formatée.
 *
 * @param c Connexion Mongoose
 * @param j Objet JSON à envoyer
 * @param code Code HTTP de réponse
 */
static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
    char *text = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
    free(text);
    if (j) cJSON_Delete(j);
}

/**
 * Vérifie si l utilisateur courant est administrateur.
 *
 * @param hm Message HTTP contenant le cookie
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* hm) {
    int uid = get_current_user_id(hm);
    return user_repo_is_admin(DB, uid);
}

/**
 * Récupère une plante par son ID.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 * @param id ID de la plante
 */
void plant_get(struct mg_connection* c, struct mg_http_message *hm, int id) {
    Plant p;
    if (!plant_repo_find(DB, id, &p)) {
        mg_http_reply(c, 404, "", "");
        return;
    }
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", p.id);
    cJSON_AddStringToObject(j, "name", p.name);
    cJSON_AddStringToObject(j, "description", p.description);
    cJSON_AddNumberToObject(j, "price", p.price);
    cJSON_AddNumberToObject(j, "stock", p.stock);
    send_json_reply(c, j, 200);
    (void)hm;
}

/**
 * Callback pour ajouter une plante à un tableau JSON.
 *
 * @param p Pointeur vers la plante
 * @param a Tableau JSON cible
 */
static void admin_plants_list_cb(Plant* p, void* a) {
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", p->id);
    cJSON_AddStringToObject(j, "name", p->name);
    cJSON_AddNumberToObject(j, "price", p->price);
    cJSON_AddNumberToObject(j, "stock", p->stock);
    cJSON_AddItemToArray((cJSON*)a, j);
}

/**
 * Liste toutes les plantes (accès public).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void plants_list_public(struct mg_connection* c, struct mg_http_message *hm) {
    cJSON *arr = cJSON_CreateArray();
    plant_repo_each(DB, admin_plants_list_cb, arr);
    send_json_reply(c, arr, 200);
}

/**
 * Liste toutes les plantes (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void admin_plants_list(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    cJSON *arr = cJSON_CreateArray();
    plant_repo_each(DB, admin_plants_list_cb, arr);
    send_json_reply(c, arr, 200);
}

/**
 * Ajoute une nouvelle plante (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les données JSON
 */
void admin_plants_add(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

		Plant p = {0};
		const char *name = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
		const char *desc = cJSON_GetStringValue(cJSON_GetObjectItem(j, "description"));
		if (name) strncpy(p.name, name, sizeof(p.name) - 1);
		if (desc) strncpy(p.description, desc, sizeof(p.description) - 1);
		p.price = cJSON_GetObjectItem(j, "price")->valueint;
		p.stock = cJSON_GetObjectItem(j, "stock")->valueint;
		p.id = plant_repo_add(DB, &p);
    cJSON_Delete(j);

    cJSON *o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", p.id);
    send_json_reply(c, o, 201);
}

/**
 * Modifie une plante existante (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les données JSON
 * @param id ID de la plante à modifier
 */
void admin_plants_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    plant_repo_patch(DB, id, j);
    cJSON_Delete(j);
    mg_http_reply(c, 200, "", "");
}

/**
 * Supprime une plante (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 * @param id ID de la plante à supprimer
 */
void admin_plants_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    plant_repo_del(DB, id);
    mg_http_reply(c, 200, "", "");
}
