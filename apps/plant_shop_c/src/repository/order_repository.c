#include "order_repository.h"
#include "order_item_repository.h"
#include "plant_repository.h"
#include "user_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

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
        fprintf(stderr, "order_repo_add failed: %s\n", PQerrorMessage(c));
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

cJSON* order_repo_list(PGconn *c, int uid) {
    cJSON *arr = cJSON_CreateArray();
    char s[12];
    sprintf(s, "%d", uid);
    const char *v[1] = {s};
    PGresult *r = PQexecParams(c, "SELECT id,user_id,total,status FROM orders WHERE user_id=$1", 1, NULL, v, NULL, NULL, 0);
    for (int i = 0; i < PQntuples(r); i++) {
        cJSON *j = cJSON_CreateObject();
        cJSON_AddNumberToObject(j, "id", atoi(PQgetvalue(r, i, 0)));
        cJSON_AddNumberToObject(j, "userId", atoi(PQgetvalue(r, i, 1)));
        cJSON_AddNumberToObject(j, "total", atoi(PQgetvalue(r, i, 2)));
        cJSON_AddStringToObject(j, "status", PQgetvalue(r, i, 3));
        cJSON_AddItemToArray(arr, j);
    }
    PQclear(r);
    return arr;
}

void order_repo_patch(PGconn *c, int id, cJSON *j) {
    char sid[12];
    sprintf(sid, "%d", id);
    cJSON *status = cJSON_GetObjectItem(j, "status");
    if (status && cJSON_IsString(status)) {
        const char *v[2] = {status->valuestring, sid};
        PQclear(PQexecParams(c, "UPDATE orders SET status=$1 WHERE id=$2", 2, NULL, v, NULL, NULL, 0));
    }
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
