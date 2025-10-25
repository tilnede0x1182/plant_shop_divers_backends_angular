#include "plant_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/plant_repository.h"
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

static void out_json(struct http_request* r, cJSON* j, int c) {
    char* t = cJSON_PrintUnformatted(j);
    http_response(r, c, t, strlen(t));
    free(t);
    cJSON_Delete(j);
}

static int is_admin(struct http_request* req) {
    const char *ck = NULL;
    if (!http_request_header(req, "cookie", &ck) || !ck) {
        return 0;
    }
    const char* jwt_val = strstr(ck, "jwt=");
    if (!jwt_val) return 0;
    int uid = atoi(jwt_val + 4);
    return user_repo_is_admin(DB, uid);
}

int plant_get(struct http_request* req) {
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    Plant p;
    if (!plant_repo_find(DB, id, &p)) { http_response(req, 404, NULL, 0); return KORE_RESULT_OK; }
    cJSON *j = cJSON_CreateObject();
    cJSON_AddStringToObject(j, "name", p.name);
    cJSON_AddNumberToObject(j, "price", p.price);
    cJSON_AddNumberToObject(j, "id", p.id);
    // Le test n'attend pas le stock, on ne l'ajoute pas.
    out_json(req, j, 200);
    return KORE_RESULT_OK;
}

static void admin_plants_list_cb(Plant* p, void* a) {
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", p->id);
    cJSON_AddStringToObject(j, "name", p->name);
    cJSON_AddNumberToObject(j, "price", p->price);
    cJSON_AddNumberToObject(j, "stock", p->stock);
    cJSON_AddItemToArray((cJSON*)a, j);
}

int admin_plants_list(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    cJSON *arr = cJSON_CreateArray();
    plant_repo_each(DB, admin_plants_list_cb, arr);
    out_json(req, arr, 200);
    return KORE_RESULT_OK;
}

int admin_plants_add(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }

    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    Plant p = {0};
    snprintf(p.name, sizeof(p.name), "%s", cJSON_GetStringValue(cJSON_GetObjectItem(j, "name")));
    p.price = cJSON_GetObjectItem(j, "price")->valueint;
    p.stock = cJSON_GetObjectItem(j, "stock")->valueint;
    p.id = plant_repo_add(DB, &p);
    cJSON_Delete(j);
    cJSON *o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", p.id);
    out_json(req, o, 201);
    return KORE_RESULT_OK;
}

int admin_plants_patch(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }

    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    plant_repo_patch(DB, id, j);
    cJSON_Delete(j);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}

int admin_plants_del(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    plant_repo_del(DB, id);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}
