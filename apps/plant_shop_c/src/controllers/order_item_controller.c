#include "order_item_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_item_repository.h"
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include <kore/kore.h>
#include <stdint.h>

extern PGconn* DB;

// Fonction utilitaire pour lire le corps de la requête dans un buffer
static int read_body_to_buf(struct http_request *req, struct kore_buf *buf) {
    ssize_t ret;
    u_int8_t tmp[1024];
    if (http_body_rewind(req) != KORE_RESULT_OK) return KORE_RESULT_ERROR;
    for (;;) {
        ret = http_body_read(req, tmp, sizeof(tmp));
        if (ret == -1) return KORE_RESULT_ERROR;
        if (ret == 0) break;
        kore_buf_append(buf, tmp, ret);
    }
    return KORE_RESULT_OK;
}

static int current_uid(struct http_request* req) {
    const char *ck = NULL;
    if (!http_request_header(req, "cookie", &ck) || !ck) {
        return 0;
    }
    const char* jwt_val = strstr(ck, "jwt=");
    if (!jwt_val) return 0;
    return atoi(jwt_val + 4);
}

static int is_admin(struct http_request* req) {
    return user_repo_is_admin(DB, current_uid(req));
}

static void json_out(struct http_request* r, cJSON* j, int code) {
    char *txt = cJSON_PrintUnformatted(j);
    http_response(r, code, txt, strlen(txt));
    free(txt);
    cJSON_Delete(j);
}

static void order_items_by_order_cb(OrderItem *it, void *ud) {
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", it->id);
    cJSON_AddNumberToObject(j, "plantId", it->plant_id);
    cJSON_AddNumberToObject(j, "quantity", it->qty);
    cJSON_AddNumberToObject(j, "price", it->price);
    cJSON_AddItemToArray((cJSON*)ud, j);
}

int order_items_by_order(struct http_request* req) {
    int oid;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &oid)) {
        http_response(req, 400, "Invalid Order ID", 16);
        return KORE_RESULT_OK;
    }
    int uid = current_uid(req);
    if (!uid) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }
    if (!is_admin(req) && !order_repo_belongs_to(DB, oid, uid)) {
        http_response(req, 403, NULL, 0);
        return KORE_RESULT_OK;
    }
    cJSON *arr = cJSON_CreateArray();
    order_item_repo_by_order(DB, oid, order_items_by_order_cb, arr);
    json_out(req, arr, 200);
    return KORE_RESULT_OK;
}

int order_item_patch(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid Item ID", 15);
        return KORE_RESULT_OK;
    }

    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON *upd = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!upd) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    order_item_repo_patch(DB, id, upd);
    cJSON_Delete(upd);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}

int order_item_del(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid Item ID", 15);
        return KORE_RESULT_OK;
    }
    order_item_repo_del(DB, id);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}
