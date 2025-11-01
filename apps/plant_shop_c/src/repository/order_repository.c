#include "order_repository.h"
#include "order_item_repository.h"
#include "plant_repository.h"
#include "user_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

int order_repo_update_status(PGconn *conn, int order_id, const char* status) {
    char id_str[12];
    sprintf(id_str, "%d", order_id);
    const char *params[2] = {status, id_str};
    PGresult *r = PQexecParams(conn, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0);
    if (PQresultStatus(r) != PGRES_COMMAND_OK) {
        // fprintf(stderr, "order_repo_update_status failed: %s\n", PQerrorMessage(conn));
        PQclear(r);
        return 0;
    }
    PQclear(r);
    return 1;
}

int order_repo_add(PGconn *c, int user_id, cJSON* items_json) {
    int total = 0;
    cJSON* item = NULL;

    char uid_str[12], total_str[12];
    sprintf(uid_str, "%d", user_id);
    sprintf(total_str, "%d", 0); // Total initial à 0
    const char *v[3] = {uid_str, total_str, "pending"};
    PGresult *r = PQexecParams(c,
        "INSERT INTO orders(user_id,total,status) VALUES($1,$2,$3) RETURNING id",
        3, NULL, v, NULL, NULL, 0);

    if (PQresultStatus(r) != PGRES_TUPLES_OK) {
        // fprintf(stderr, "order_repo_add failed: %s\n", PQerrorMessage(c));
        PQclear(r);
        return 0;
    }
    int order_id = atoi(PQgetvalue(r, 0, 0));
    PQclear(r);

    cJSON_ArrayForEach(item, items_json) {
        OrderItem oi = {0};
        oi.order_id = order_id;
        oi.plant_id = cJSON_GetObjectItem(item, "plantId")->valueint;
        oi.qty = cJSON_GetObjectItem(item, "quantity")->valueint;
        Plant p;
        if (plant_repo_find(c, oi.plant_id, &p)) {
            oi.price = p.price;
            order_item_repo_add(c, &oi);
            total += oi.price * oi.qty;
        }
    }

    char order_id_str[12], new_total_str[12];
    sprintf(order_id_str, "%d", order_id);
    sprintf(new_total_str, "%d", total);
    const char *update_params[2] = {new_total_str, order_id_str};
    r = PQexecParams(c, "UPDATE orders SET total = $1 WHERE id = $2", 2, NULL, update_params, NULL, NULL, 0);
    PQclear(r);

    return order_id;
}

/* ---------- helpers ---------- */
struct _item_ctx { PGconn *db; cJSON *dst; };

static void format_timestamp_iso(const char *pg_ts, char *out, size_t out_sz) {
	if (!pg_ts || !out || out_sz == 0) {
		if (out && out_sz > 0) out[0] = '\0';
		return;
	}
	int year, month, day, hour, minute, second;
	if (sscanf(pg_ts, "%d-%d-%d %d:%d:%d", &year, &month, &day, &hour, &minute, &second) == 6) {
		snprintf(out, out_sz, "%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second);
	} else {
		/* fallback: copy raw value */
		snprintf(out, out_sz, "%.*s", (int)(out_sz - 1), pg_ts);
	}
}
static void _item_to_json(OrderItem *it, void *ud) {
	struct _item_ctx *ctx = ud;
	Plant p = {0};
	if (!plant_repo_find(ctx->db, it->plant_id, &p)) return;

	cJSON *itm = cJSON_CreateObject();
	cJSON_AddNumberToObject(itm, "id", it->id);
	cJSON_AddNumberToObject(itm, "quantity", it->qty);
	cJSON_AddNumberToObject(itm, "price", it->price);

	cJSON *pl = cJSON_CreateObject();
	cJSON_AddNumberToObject(pl, "id", p.id);
	cJSON_AddStringToObject(pl, "name", p.name);
	cJSON_AddNumberToObject(pl, "price", p.price);
	cJSON_AddItemToObject(itm, "plant", pl);

	cJSON_AddItemToArray(ctx->dst, itm);
}

/* ---------- liste des commandes utilisateur ----------
   Affichage : ORDER BY id DESC (plus récentes en premier)
   Numérotation : row_number() OVER (ORDER BY id ASC) AS number
   => la colonne 'number' vaut 1 pour la plus ancienne, N pour la plus récente.
*/
cJSON* order_repo_list(PGconn *c, int uid) {
	char uid_str[12]; sprintf(uid_str, "%d", uid);
	const char *params[1] = {uid_str};

	PGresult *r = PQexecParams(
		c,
		"SELECT id,total,status,created_at, row_number() OVER (ORDER BY created_at ASC) AS number "
		"FROM orders WHERE user_id=$1 ORDER BY created_at DESC",
		1, NULL, params, NULL, NULL, 0);

	/* protection basique en cas d'erreur SQL */
	if (PQresultStatus(r) != PGRES_TUPLES_OK) {
		PQclear(r);
		return cJSON_CreateArray();
	}

	cJSON *out = cJSON_CreateArray();
	for (int i = 0; i < PQntuples(r); i++) {
		int oid = atoi(PQgetvalue(r, i, 0));
		double total = atof(PQgetvalue(r, i, 1));
		const char *status = PQgetvalue(r, i, 2);
		const char *created_raw = PQgetvalue(r, i, 3);
		int number = atoi(PQgetvalue(r, i, 4)); /* 1 = plus ancienne */

		cJSON *j = cJSON_CreateObject();
		cJSON_AddNumberToObject(j, "id", oid);
		cJSON_AddNumberToObject(j, "userId", uid);
		cJSON_AddNumberToObject(j, "totalPrice", total);
		cJSON_AddStringToObject(j, "status", status);
		char created_iso[32];
		format_timestamp_iso(created_raw, created_iso, sizeof(created_iso));
		cJSON_AddStringToObject(j, "createdAt", created_iso);
		cJSON_AddNumberToObject(j, "number", number); /* nouvelle propriété */

		/* items */
		cJSON *items = cJSON_CreateArray();
		struct _item_ctx ctx = { .db = c, .dst = items };
		order_item_repo_by_order(c, oid, _item_to_json, &ctx);
		cJSON_AddItemToObject(j, "orderItems", items);

		cJSON_AddItemToArray(out, j);
	}
	PQclear(r);
	return out;
}

void order_repo_patch(PGconn *c, int id, cJSON *j) {
	char sid[12];
	sprintf(sid, "%d", id);

	cJSON *status = cJSON_GetObjectItem(j, "status");
	if (!status || !cJSON_IsString(status)) {
		// fprintf(stderr, "[DEBUG][ORDER_REPO] ordre %d : champ « status » absent ou invalide\n", id);
		return;
	}

	const char *v[2] = {status->valuestring, sid};
	PGresult *r = PQexecParams(c,
	    "UPDATE orders SET status=$1 WHERE id=$2",
	    2, NULL, v, NULL, NULL, 0);

	if (PQresultStatus(r) != PGRES_COMMAND_OK) {
		// fprintf(stderr, "[ERROR][ORDER_REPO] update ordre %d -> « %s » : %s\n",
		        // id, status->valuestring, PQerrorMessage(c));
	} else {
		// fprintf(stderr, "[DEBUG][ORDER_REPO] ordre %d mis à jour -> « %s »\n",
		        // id, status->valuestring);
	}
	PQclear(r);
}

void order_repo_del(PGconn *c, int id) {
    char sid[12];
    sprintf(sid, "%d", id);
    const char *v[1] = {sid};
    PQclear(PQexecParams(c, "DELETE FROM orders WHERE id=$1", 1, NULL, v, NULL, NULL, 0));
}

int order_repo_belongs_to(PGconn *c, int order_id, int user_id) {
    char oid_str[12], uid_str[12];
    sprintf(oid_str, "%d", order_id);
    sprintf(uid_str, "%d", user_id);
    const char *v[2] = {oid_str, uid_str};
    PGresult *r = PQexecParams(c, "SELECT 1 FROM orders WHERE id=$1 AND user_id=$2", 2, NULL, v, NULL, NULL, 0);
    int found = PQntuples(r);
    PQclear(r);
    return found;
}
