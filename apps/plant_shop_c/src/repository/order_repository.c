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
 * @param conn Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param status Nouveau statut (pending, shipped, etc.)
 * @return 1 si succès, 0 sinon
 */
int order_repo_update_status(PGconn *conn, int order_id, const char* status) {
	char id_str[12];
	sprintf(id_str, "%d", order_id);
	const char *params[2] = {status, id_str};
	PGresult *res = PQexecParams(conn, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0);
	int ok = (PQresultStatus(res) == PGRES_COMMAND_OK);
	PQclear(res);
	return ok;
}

/**
 * Insère une commande vide et retourne son ID.
 *
 * @param conn Connexion PostgreSQL
 * @param user_id ID de l utilisateur
 * @return ID de la commande créée, 0 si erreur
 */
static int insert_empty_order(PGconn *conn, int user_id) {
	char uid_str[12];
	sprintf(uid_str, "%d", user_id);
	const char *params[3] = {uid_str, "0", "pending"};
	PGresult *res = PQexecParams(conn, "INSERT INTO orders(user_id,total,status) VALUES($1,$2,$3) RETURNING id", 3, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return 0; }
	int order_id = atoi(PQgetvalue(res, 0, 0));
	PQclear(res);
	return order_id;
}

/**
 * Ajoute un article à une commande et retourne le sous-total.
 *
 * @param conn Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param item JSON de l article
 * @return Sous-total (price * qty), 0 si erreur
 */
static int add_order_item(PGconn *conn, int order_id, cJSON* item) {
	OrderItem oi = {0};
	oi.order_id = order_id;
	oi.plant_id = cJSON_GetObjectItem(item, "plantId")->valueint;
	oi.qty = cJSON_GetObjectItem(item, "quantity")->valueint;
	Plant pl;
	if (!plant_repo_find(conn, oi.plant_id, &pl)) return 0;
	oi.price = pl.price;
	order_item_repo_add(conn, &oi);
	return oi.price * oi.qty;
}

/**
 * Met à jour le total d une commande.
 *
 * @param conn Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param total Nouveau total
 */
static void update_order_total(PGconn *conn, int order_id, int total) {
	char oid_str[12], total_str[12];
	sprintf(oid_str, "%d", order_id);
	sprintf(total_str, "%d", total);
	const char *params[2] = {total_str, oid_str};
	PQclear(PQexecParams(conn, "UPDATE orders SET total = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Crée une commande avec ses articles.
 *
 * @param conn Connexion PostgreSQL
 * @param user_id ID de l utilisateur
 * @param items_json Tableau JSON des articles
 * @return ID de la commande créée, 0 si erreur
 */
int order_repo_add(PGconn *conn, int user_id, cJSON* items_json) {
	int order_id = insert_empty_order(conn, user_id);
	if (order_id == 0) return 0;
	int total = 0;
	cJSON* item = NULL;
	cJSON_ArrayForEach(item, items_json) { total += add_order_item(conn, order_id, item); }
	update_order_total(conn, order_id, total);
	return order_id;
}

/* ---------- helpers ---------- */
struct _item_ctx { PGconn *db; cJSON *dst; };

/**
 * Convertit un timestamp PostgreSQL en format ISO 8601.
 *
 * @param pg_ts Timestamp PostgreSQL
 * @param out Buffer de sortie
 * @param out_sz Taille du buffer
 */
static void format_timestamp_iso(const char *pg_ts, char *out, size_t out_sz) {
	if (!pg_ts || !out || out_sz == 0) { if (out && out_sz > 0) out[0] = '\0'; return; }
	int yr, mo, dy, hr, mi, sc;
	if (sscanf(pg_ts, "%d-%d-%d %d:%d:%d", &yr, &mo, &dy, &hr, &mi, &sc) == 6) {
		snprintf(out, out_sz, "%04d-%02d-%02dT%02d:%02d:%02dZ", yr, mo, dy, hr, mi, sc);
	} else {
		snprintf(out, out_sz, "%.*s", (int)(out_sz - 1), pg_ts);
	}
}

/**
 * Crée l objet JSON d une plante.
 *
 * @param pl Pointeur vers la plante
 * @return Objet cJSON
 */
static cJSON* plant_to_json(Plant *pl) {
	cJSON *obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(obj, "id", pl->id);
	cJSON_AddStringToObject(obj, "name", pl->name);
	cJSON_AddNumberToObject(obj, "price", pl->price);
	return obj;
}

/**
 * Callback pour convertir un OrderItem en JSON.
 *
 * @param it Pointeur vers l OrderItem
 * @param ud Contexte
 */
static void _item_to_json(OrderItem *it, void *ud) {
	struct _item_ctx *ctx = ud;
	Plant pl = {0};
	if (!plant_repo_find(ctx->db, it->plant_id, &pl)) return;
	cJSON *obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(obj, "id", it->id);
	cJSON_AddNumberToObject(obj, "quantity", it->qty);
	cJSON_AddNumberToObject(obj, "price", it->price);
	cJSON_AddItemToObject(obj, "plant", plant_to_json(&pl));
	cJSON_AddItemToArray(ctx->dst, obj);
}

/**
 * Construit l objet JSON d une commande.
 *
 * @param res Résultat PostgreSQL
 * @param row Index de la ligne
 * @param uid User ID
 * @return Objet cJSON
 */
static cJSON* build_order_json(PGresult *res, int row, int uid) {
	cJSON *obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(obj, "id", atoi(PQgetvalue(res, row, 0)));
	cJSON_AddNumberToObject(obj, "userId", uid);
	cJSON_AddNumberToObject(obj, "totalPrice", atof(PQgetvalue(res, row, 1)));
	cJSON_AddStringToObject(obj, "status", PQgetvalue(res, row, 2));
	char iso[32];
	format_timestamp_iso(PQgetvalue(res, row, 3), iso, sizeof(iso));
	cJSON_AddStringToObject(obj, "createdAt", iso);
	cJSON_AddNumberToObject(obj, "number", atoi(PQgetvalue(res, row, 4)));
	return obj;
}

/**
 * Liste des commandes d un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param uid User ID
 * @return Tableau cJSON
 */
cJSON* order_repo_list(PGconn *conn, int uid) {
	char uid_str[12];
	sprintf(uid_str, "%d", uid);
	const char *params[1] = {uid_str};
	PGresult *res = PQexecParams(conn, "SELECT id,total,status,created_at, row_number() OVER (ORDER BY created_at ASC) AS number FROM orders WHERE user_id=$1 ORDER BY created_at DESC", 1, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return cJSON_CreateArray(); }
	cJSON *arr = cJSON_CreateArray();
	for (int idx = 0; idx < PQntuples(res); idx++) {
		cJSON *obj = build_order_json(res, idx, uid);
		cJSON *items = cJSON_CreateArray();
		struct _item_ctx ctx = { .db = conn, .dst = items };
		order_item_repo_by_order(conn, atoi(PQgetvalue(res, idx, 0)), _item_to_json, &ctx);
		cJSON_AddItemToObject(obj, "orderItems", items);
		cJSON_AddItemToArray(arr, obj);
	}
	PQclear(res);
	return arr;
}

/**
 * Met à jour une commande (statut uniquement).
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de la commande
 * @param json Objet JSON contenant le champ status
 */
void order_repo_patch(PGconn *conn, int id, cJSON *json) {
	cJSON *status = cJSON_GetObjectItem(json, "status");
	if (!status || !cJSON_IsString(status)) return;
	char sid[12];
	sprintf(sid, "%d", id);
	const char *params[2] = {status->valuestring, sid};
	PQclear(PQexecParams(conn, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Supprime une commande.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de la commande à supprimer
 */
void order_repo_del(PGconn *conn, int id) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *params[1] = {sid};
	PQclear(PQexecParams(conn, "DELETE FROM orders WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}

/**
 * Vérifie si une commande appartient à un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param user_id ID de l utilisateur
 * @return 1 si la commande appartient à l utilisateur, 0 sinon
 */
int order_repo_belongs_to(PGconn *conn, int order_id, int user_id) {
	char oid_str[12], uid_str[12];
	sprintf(oid_str, "%d", order_id);
	sprintf(uid_str, "%d", user_id);
	const char *params[2] = {oid_str, uid_str};
	PGresult *res = PQexecParams(conn, "SELECT 1 FROM orders WHERE id=$1 AND user_id=$2", 2, NULL, params, NULL, NULL, 0);
	int found = PQntuples(res);
	PQclear(res);
	return found;
}
