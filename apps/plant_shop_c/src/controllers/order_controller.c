/* ==============================================================================
   Importations
   ============================================================================== */
#include "order_controller.h"
#include <stdint.h>
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdio.h>
#include "../utils/utils.h"

/* ==============================================================================
   Données
   ============================================================================== */
extern PGconn* DB;

static int is_admin(struct mg_http_message* hm);

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
 * Modifie le statut d une commande (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant le nouveau statut
 * @param order_id ID de la commande
 */
void patch_order_status(struct mg_connection* c, struct mg_http_message* hm, int order_id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "Content-Type: application/json\r\n", "{\"error\":\"Forbidden\"}\n"); return; }
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}\n"); return; }
	const char* new_status = cJSON_GetStringValue(cJSON_GetObjectItem(json, "status"));
	if (!new_status) { cJSON_Delete(json); mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing status\"}\n"); return; }
	int ok = order_repo_update_status(DB, order_id, new_status);
	cJSON_Delete(json);
	if (!ok) { mg_http_reply(c, 500, "Content-Type: application/json\r\n", "{\"error\":\"Update failed\"}\n"); return; }
	mg_http_reply(c, 200, "Content-Type: application/json\r\n", "{\"status\":\"%s\"}\n", new_status);
}

/**
 * Cree une nouvelle commande.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les articles
 */
void orders_create(struct mg_connection* c, struct mg_http_message *hm) {
	int user_id = get_current_user_id(hm);
	if (!user_id) { mg_http_reply(c, 401, "", ""); return; }
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	cJSON *items = cJSON_GetObjectItem(json, "items");
	if (!items || !cJSON_IsArray(items)) { cJSON_Delete(json); mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing items\"}"); return; }
	int new_order_id = order_repo_add(DB, user_id, items);
	cJSON_Delete(json);
	cJSON* response = cJSON_CreateObject();
	cJSON_AddNumberToObject(response, "id", new_order_id);
	send_json_reply(c, response, 201);
}

/**
 * Liste les commandes de l utilisateur.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
void orders_list(struct mg_connection* c, struct mg_http_message *hm) {
	int user_id = get_current_user_id(hm);
	if (!user_id) { mg_http_reply(c, 401, "", ""); return; }
	cJSON* arr = order_repo_list(DB, user_id);
	send_json_reply(c, arr, 200);
}

/**
 * Modifie une commande existante (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les donnees
 * @param id ID de la commande
 */
void orders_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON *json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	order_repo_patch(DB, id, json);
	cJSON_Delete(json);
	mg_http_reply(c, 200, "", "");
}

/**
 * Supprime une commande (acces admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id ID de la commande
 */
void orders_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	order_repo_del(DB, id);
	mg_http_reply(c, 200, "", "");
}
