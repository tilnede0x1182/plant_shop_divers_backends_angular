#include "order_controller.h"
#include <kore/kore.h>
#include <stdint.h>
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"

extern PGconn* DB;


/** """ lit entièrement le body HTTP dans un kore_buf """ */
static int read_body_to_buf(struct http_request *req, struct kore_buf *buf){
	ssize_t ret; u_int8_t tmp[1024];
	if (http_body_rewind(req) != KORE_RESULT_OK) return KORE_RESULT_ERROR;
	for (;;){
		ret = http_body_read(req, tmp, sizeof tmp);
		if (ret == -1) return KORE_RESULT_ERROR;
		if (ret == 0) break;
		kore_buf_append(buf, tmp, ret);
	}
	return KORE_RESULT_OK;
}

static int current_uid(struct http_request* req) {
    const char* ck = NULL;
    http_request_header(req, "cookie", &ck);
    if (!ck) return 0;
    const char* jwt_val = strstr(ck, "jwt=");
    if (!jwt_val) return 0;
    return atoi(jwt_val + 4);
}

static int admin(struct http_request* req) {
    return user_repo_is_admin(DB, current_uid(req));
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

		struct kore_buf *buf = kore_buf_alloc(0);
		if (read_body_to_buf(req, buf) != KORE_RESULT_OK){
			kore_buf_free(buf);
			http_response(req, 500, "Cannot read body", 15);
			return KORE_RESULT_OK;
		}
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    cJSON *items = cJSON_GetObjectItem(j, "items");
    if (!items || !cJSON_IsArray(items)) {
        cJSON_Delete(j);
        http_response(req, 400, "Missing 'items' array", 21);
        return KORE_RESULT_OK;
    }

    int oid = order_repo_add(DB, uid, items);
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

		struct kore_buf *buf = kore_buf_alloc(0);
		if (read_body_to_buf(req, buf) != KORE_RESULT_OK){
			kore_buf_free(buf);
			http_response(req, 500, "Cannot read body", 15);
			return KORE_RESULT_OK;
		}
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

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
