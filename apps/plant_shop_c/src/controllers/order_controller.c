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
extern PGconn* DATABASE_CONNECTION;

static int is_admin(struct mg_http_message* http_message);

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
static int is_admin(struct mg_http_message* http_message) {
	int user_identifier = get_current_user_id(http_message);
	return user_repo_is_admin(DATABASE_CONNECTION, user_identifier);
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Modifie le statut d une commande (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant le nouveau statut
 * @param order_id ID de la commande
 */
void patch_order_status(struct mg_connection* mongoose_connection, struct mg_http_message* http_message, int order_identifier) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "Content-Type: application/json\r\n", "{\"error\":\"Forbidden\"}\n"); return; }
	cJSON* json_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_data) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}\n"); return; }
	const char* new_status = cJSON_GetStringValue(cJSON_GetObjectItem(json_data, "status"));
	if (!new_status) { cJSON_Delete(json_data); mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing status\"}\n"); return; }
	int update_success = order_repo_update_status(DATABASE_CONNECTION, order_identifier, new_status);
	cJSON_Delete(json_data);
	if (!update_success) { mg_http_reply(mongoose_connection, 500, "Content-Type: application/json\r\n", "{\"error\":\"Update failed\"}\n"); return; }
	mg_http_reply(mongoose_connection, 200, "Content-Type: application/json\r\n", "{\"status\":\"%s\"}\n", new_status);
}

/**
 * Cree une nouvelle commande.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les articles
 */
void orders_create(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	int user_identifier = get_current_user_id(http_message);
	if (!user_identifier) { mg_http_reply(mongoose_connection, 401, "", ""); return; }
	cJSON* json_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_data) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	cJSON *items_array = cJSON_GetObjectItem(json_data, "items");
	if (!items_array || !cJSON_IsArray(items_array)) { cJSON_Delete(json_data); mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing items\"}"); return; }
	int new_order_identifier = order_repo_add(DATABASE_CONNECTION, user_identifier, items_array);
	cJSON_Delete(json_data);
	cJSON* response_json = cJSON_CreateObject();
	cJSON_AddNumberToObject(response_json, "id", new_order_identifier);
	send_json_reply(mongoose_connection, response_json, 201);
}

/**
 * Liste les commandes de l utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void orders_list(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	int user_identifier = get_current_user_id(http_message);
	if (!user_identifier) { mg_http_reply(mongoose_connection, 401, "", ""); return; }
	cJSON* orders_array = order_repo_list(DATABASE_CONNECTION, user_identifier);
	send_json_reply(mongoose_connection, orders_array, 200);
}

/**
 * Modifie une commande existante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees
 * @param order_identifier ID de la commande
 */
void orders_patch(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int order_identifier) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON *json_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_data) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	order_repo_patch(DATABASE_CONNECTION, order_identifier, json_data);
	cJSON_Delete(json_data);
	mg_http_reply(mongoose_connection, 200, "", "");
}

/**
 * Supprime une commande (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param order_identifier Identifiant de la commande
 */
void orders_del(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int order_identifier) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	order_repo_del(DATABASE_CONNECTION, order_identifier);
	mg_http_reply(mongoose_connection, 200, "", "");
}
