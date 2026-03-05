/* ==============================================================================
   Importations
   ============================================================================== */
#include "order_repository.h"
#include "order_item_repository.h"
#include "plant_repository.h"
#include "user_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Met à jour le statut d une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param status Nouveau statut (pending, shipped, etc.)
 * @return 1 si succès, 0 sinon
 */
int order_repo_update_status(PGconn *database_connection, int order_id, const char* status) {
	char id_string[12];
	sprintf(id_string, "%d", order_id);
	const char *params[2] = {status, id_string};
	PGresult *query_result = PQexecParams(database_connection, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0);
	int ok = (PQresultStatus(query_result) == PGRES_COMMAND_OK);
	PQclear(query_result);
	return ok;
}

/**
 * Insère une commande vide et retourne son ID.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_id ID de l utilisateur
 * @return ID de la commande créée, 0 si erreur
 */
static int insert_empty_order(PGconn *database_connection, int user_id) {
	char user_id_string[12];
	sprintf(user_id_string, "%d", user_id);
	const char *params[3] = {user_id_string, "0", "pending"};
	PGresult *query_result = PQexecParams(database_connection, "INSERT INTO orders(user_id,total,status) VALUES($1,$2,$3) RETURNING id", 3, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return 0; }
	int order_id = atoi(PQgetvalue(query_result, 0, 0));
	PQclear(query_result);
	return order_id;
}

/**
 * Ajoute un article à une commande et retourne le sous-total.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param json_item JSON de l article
 * @return Sous-total (price * quantity), 0 si erreur
 */
static int add_order_item(PGconn *database_connection, int order_id, cJSON* json_item) {
	OrderItem order_item = {0};
	order_item.order_id = order_id;
	order_item.plant_id = cJSON_GetObjectItem(json_item, "plantId")->valueint;
	order_item.qty = cJSON_GetObjectItem(json_item, "quantity")->valueint;
	Plant plant_data;
	if (!plant_repo_find(database_connection, order_item.plant_id, &plant_data)) return 0;
	order_item.price = plant_data.price;
	order_item_repo_add(database_connection, &order_item);
	return order_item.price * order_item.qty;
}

/**
 * Met à jour le total d une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param total Nouveau total
 */
static void update_order_total(PGconn *database_connection, int order_id, int total) {
	char order_id_string[12], total_string[12];
	sprintf(order_id_string, "%d", order_id);
	sprintf(total_string, "%d", total);
	const char *params[2] = {total_string, order_id_string};
	PQclear(PQexecParams(database_connection, "UPDATE orders SET total = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Crée une commande avec ses articles.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_id ID de l utilisateur
 * @param items_json Tableau JSON des articles
 * @return ID de la commande créée, 0 si erreur
 */
int order_repo_add(PGconn *database_connection, int user_id, cJSON* items_json) {
	int order_id = insert_empty_order(database_connection, user_id);
	if (order_id == 0) return 0;
	int total = 0;
	cJSON* json_item = NULL;
	cJSON_ArrayForEach(json_item, items_json) { total += add_order_item(database_connection, order_id, json_item); }
	update_order_total(database_connection, order_id, total);
	return order_id;
}

/* ---------- helpers ---------- */
struct _item_ctx { PGconn *database_connection; cJSON *destination_array; };

/**
 * Convertit un timestamp PostgreSQL en format ISO 8601.
 *
 * @param pg_ts Timestamp PostgreSQL
 * @param out Buffer de sortie
 * @param out_sz Taille du buffer
 */
static void format_timestamp_iso(const char *pg_ts, char *out, size_t out_sz) {
	if (!pg_ts || !out || out_sz == 0) { if (out && out_sz > 0) out[0] = '\0'; return; }
	int year, month, day, hour, minute, second;
	if (sscanf(pg_ts, "%d-%d-%d %d:%d:%d", &year, &month, &day, &hour, &minute, &second) == 6) {
		snprintf(out, out_sz, "%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second);
	} else {
		snprintf(out, out_sz, "%.*s", (int)(out_sz - 1), pg_ts);
	}
}

/**
 * Crée l objet JSON d une plante.
 *
 * @param plant_data Pointeur vers la plante
 * @return Objet cJSON
 */
static cJSON* plant_to_json(Plant *plant_data) {
	cJSON *json_object = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_object, "id", plant_data->id);
	cJSON_AddStringToObject(json_object, "name", plant_data->name);
	cJSON_AddNumberToObject(json_object, "price", plant_data->price);
	return json_object;
}

/**
 * Callback pour convertir un OrderItem en JSON.
 *
 * @param order_item Pointeur vers l OrderItem
 * @param callback_data Contexte
 */
static void _item_to_json(OrderItem *order_item, void *callback_data) {
	struct _item_ctx *item_context = callback_data;
	Plant plant_data = {0};
	if (!plant_repo_find(item_context->database_connection, order_item->plant_id, &plant_data)) return;
	cJSON *json_object = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_object, "id", order_item->id);
	cJSON_AddNumberToObject(json_object, "quantity", order_item->qty);
	cJSON_AddNumberToObject(json_object, "price", order_item->price);
	cJSON_AddItemToObject(json_object, "plant", plant_to_json(&plant_data));
	cJSON_AddItemToArray(item_context->destination_array, json_object);
}

/**
 * Construit l objet JSON d une commande.
 *
 * @param query_result Résultat PostgreSQL
 * @param row_index Index de la ligne
 * @param user_id User ID
 * @return Objet cJSON
 */
static cJSON* build_order_json(PGresult *query_result, int row_index, int user_id) {
	cJSON *json_object = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_object, "id", atoi(PQgetvalue(query_result, row_index, 0)));
	cJSON_AddNumberToObject(json_object, "userId", user_id);
	cJSON_AddNumberToObject(json_object, "totalPrice", atof(PQgetvalue(query_result, row_index, 1)));
	cJSON_AddStringToObject(json_object, "status", PQgetvalue(query_result, row_index, 2));
	char iso_timestamp[32];
	format_timestamp_iso(PQgetvalue(query_result, row_index, 3), iso_timestamp, sizeof(iso_timestamp));
	cJSON_AddStringToObject(json_object, "createdAt", iso_timestamp);
	cJSON_AddNumberToObject(json_object, "number", atoi(PQgetvalue(query_result, row_index, 4)));
	return json_object;
}

/**
 * Liste des commandes d un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param uid User ID
 * @return Tableau cJSON
 */
cJSON* order_repo_list(PGconn *database_connection, int user_id) {
	char user_id_string[12];
	sprintf(user_id_string, "%d", user_id);
	const char *params[1] = {user_id_string};
	PGresult *query_result = PQexecParams(database_connection, "SELECT id,total,status,created_at, row_number() OVER (ORDER BY created_at ASC) AS number FROM orders WHERE user_id=$1 ORDER BY created_at DESC", 1, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return cJSON_CreateArray(); }
	cJSON *json_array = cJSON_CreateArray();
	for (int row_index = 0; row_index < PQntuples(query_result); row_index++) {
		cJSON *json_object = build_order_json(query_result, row_index, user_id);
		cJSON *items_array = cJSON_CreateArray();
		struct _item_ctx item_context = { .database_connection = database_connection, .destination_array = items_array };
		order_item_repo_by_order(database_connection, atoi(PQgetvalue(query_result, row_index, 0)), _item_to_json, &item_context);
		cJSON_AddItemToObject(json_object, "orderItems", items_array);
		cJSON_AddItemToArray(json_array, json_object);
	}
	PQclear(query_result);
	return json_array;
}

/**
 * Met à jour une commande (statut uniquement).
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param json_data Objet JSON contenant le champ status
 */
void order_repo_patch(PGconn *database_connection, int order_id, cJSON *json_data) {
	cJSON *status_value = cJSON_GetObjectItem(json_data, "status");
	if (!status_value || !cJSON_IsString(status_value)) return;
	char string_id[12];
	sprintf(string_id, "%d", order_id);
	const char *params[2] = {status_value->valuestring, string_id};
	PQclear(PQexecParams(database_connection, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Supprime une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande à supprimer
 */
void order_repo_del(PGconn *database_connection, int order_id) {
	char string_id[12];
	sprintf(string_id, "%d", order_id);
	const char *params[1] = {string_id};
	PQclear(PQexecParams(database_connection, "DELETE FROM orders WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}

/**
 * Vérifie si une commande appartient à un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param user_id ID de l utilisateur
 * @return 1 si la commande appartient à l utilisateur, 0 sinon
 */
int order_repo_belongs_to(PGconn *database_connection, int order_id, int user_id) {
	char order_id_string[12], user_id_string[12];
	sprintf(order_id_string, "%d", order_id);
	sprintf(user_id_string, "%d", user_id);
	const char *params[2] = {order_id_string, user_id_string};
	PGresult *query_result = PQexecParams(database_connection, "SELECT 1 FROM orders WHERE id=$1 AND user_id=$2", 2, NULL, params, NULL, NULL, 0);
	int found = PQntuples(query_result);
	PQclear(query_result);
	return found;
}
