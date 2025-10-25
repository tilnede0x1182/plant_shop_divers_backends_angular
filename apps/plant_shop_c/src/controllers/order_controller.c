#include "order_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_repository.h"

extern PGconn* DB;

static int current_uid(struct http_request* req) {
    const char* ck = http_request_header(req, "cookie");
    if (!ck) return 0;
    const char* jwt_val = strstr(ck, "jwt=");
    if (!jwt_val) return 0;
    return atoi(jwt_val + 4);
}

static int admin(struct http_request* req) {
    return order_repo_is_admin(DB, current_uid(req));
}

static void jout(struct http_request* r, cJSON* j, int c) {
    char* t = cJSON_PrintUnformatted(j);
    http_response(r, c, t, strlen(t));
    free(t);
    cJSON_Delete(j);
}

int orders_create(struct http_request* req) {
    int uid = current_uid(req);
    if (!uid) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }
    size_t len;
    const uint8_t* b = http_body_read(req, &len);
    cJSON* j = cJSON_ParseWithLength((const char*)b, len);
    if (!j) { http_response(req, 400, NULL, 0); return KORE_RESULT_OK; }
    int oid = order_repo_add(DB, uid, j);
    cJSON_Delete(j);
    cJSON* o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", oid);
    jout(req, o, 201);
    return KORE_RESULT_OK;
}

int orders_list(struct http_request* req) {
    int uid = current_uid(req);
    if (!uid) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }
    cJSON* arr = order_repo_list(DB, uid);
    jout(req, arr, 200);
    return KORE_RESULT_OK;
}

int orders_patch(struct http_request* req) {
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    if (!admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    size_t len;
    const uint8_t* b = http_body_read(req, &len);
    cJSON* j = cJSON_ParseWithLength((const char*)b, len);
    order_repo_patch(DB, id, j);
    cJSON_Delete(j);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}

int orders_del(struct http_request* req) {
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    if (!admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    order_repo_del(DB, id);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}
