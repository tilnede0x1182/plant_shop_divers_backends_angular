#include "auth_controller.h"
#include <kore/kore.h>
#include <stdint.h>
#include <cjson/cJSON.h>
#include <argon2.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include "../repository/user_repository.h"

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

static void send_json(struct http_request* req, cJSON* j, int code) {
    char* txt = cJSON_PrintUnformatted(j);
    http_response(req, code, txt, strlen(txt));
    free(txt);
    cJSON_Delete(j);
}

static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = rand();
    }
}

int auth_register(struct http_request* req) {
    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    const char *n = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
    const char *e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char *p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    if (!n || !e || !p) {
        cJSON_Delete(j);
        http_response(req, 400, "Missing fields", 14);
        return KORE_RESULT_OK;
    }

    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));

    char encoded_hash[128];
    if (argon2id_hash_encoded(2, 1 << 16, 1, p, strlen(p), salt, sizeof(salt), 32, encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
        cJSON_Delete(j);
        http_response(req, 500, "Hashing failed", 14);
        return KORE_RESULT_OK;
    }

    User u = {.is_admin = 0};
    snprintf(u.name, sizeof(u.name), "%s", n);
    snprintf(u.email, sizeof(u.email), "%s", e);
    snprintf(u.password_hash, sizeof(u.password_hash), "%s", encoded_hash);

    u.id = user_repo_add(DB, &u);
    cJSON_Delete(j);

    cJSON *out = cJSON_CreateObject();
    cJSON_AddNumberToObject(out, "id", u.id);
    send_json(req, out, 201);
    return KORE_RESULT_OK;
}

int auth_login(struct http_request* req) {
    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    const char* e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char* p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));
    cJSON_Delete(j);

    if (!e || !p) { http_response(req, 400, "Missing fields", 14); return KORE_RESULT_OK; }

    User u;
    if (!user_repo_find_by_mail(DB, e, &u)) {
        http_response(req, 401, "Invalid credentials", 19);
        return KORE_RESULT_OK;
    }

    if (argon2id_verify(u.password_hash, p, strlen(p)) != ARGON2_OK) {
        http_response(req, 401, "Invalid credentials", 19);
        return KORE_RESULT_OK;
    }

    char cookie[64];
    sprintf(cookie, "jwt=%d; Path=/; HttpOnly", u.id);
    http_response_header(req, "Set-Cookie", cookie);
    cJSON *o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    send_json(req, o, 201);
    return KORE_RESULT_OK;
}

int auth_me(struct http_request* req) {
    const char *hdr = NULL;
    http_request_header(req, "cookie", &hdr);
    if (!hdr) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }

    const char* jwt_val = strstr(hdr, "jwt=");
    if (!jwt_val) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }

    int uid = atoi(jwt_val + 4);
    if (uid == 0) {
        http_response(req, 401, "Invalid token", 13);
        return KORE_RESULT_OK;
    }
    User u;
    if (!user_repo_find(DB, uid, &u)) {
        http_response(req, 401, "User not found", 14);
        return KORE_RESULT_OK;
    }

    cJSON* o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    cJSON_AddStringToObject(o, "name", u.name);
    cJSON_AddNumberToObject(o, "id", u.id);
    send_json(req, o, 200);
    return KORE_RESULT_OK;
}
