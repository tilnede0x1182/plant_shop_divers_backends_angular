#include "order_item_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_item_repository.h"
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdint.h>
#include "../utils/utils.h"

extern PGconn* DB;

static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
    char *text = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
    free(text);
    if (j) cJSON_Delete(j);
}

static int is_admin(struct mg_http_message* hm) {
    int uid = get_current_user_id(hm);
    return user_repo_is_admin(DB, uid);
}

static void order_items_by_order_cb(OrderItem *it, void *ud) {
    cJSON *arr = (cJSON*)ud;
    cJSON* j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", it->id);
    cJSON_AddNumberToObject(j, "plantId", it->plant_id);
    cJSON_AddNumberToObject(j, "quantity", it->qty);
    cJSON_AddNumberToObject(j, "price", it->price);
    cJSON_AddItemToArray(arr, j);
}

void order_items_by_order(struct mg_connection *c, struct mg_http_message *hm, int order_id) {
    int uid = get_current_user_id(hm);
    if (!uid) {
        mg_http_reply(c, 401, "", "");
        return;
    }
    if (!is_admin(hm) && !order_repo_belongs_to(DB, order_id, uid)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    cJSON *arr = cJSON_CreateArray();
    order_item_repo_by_order(DB, order_id, order_items_by_order_cb, arr);
    send_json_reply(c, arr, 200);
}

void order_item_patch(struct mg_connection *c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON *upd = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!upd) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    order_item_repo_patch(DB, id, upd);
    cJSON_Delete(upd);
    mg_http_reply(c, 200, "", "");
}

void order_item_del(struct mg_connection *c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    order_item_repo_del(DB, id);
    mg_http_reply(c, 200, "", "");
}
