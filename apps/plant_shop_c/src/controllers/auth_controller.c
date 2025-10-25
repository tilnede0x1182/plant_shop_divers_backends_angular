#include "auth_controller.h"
#include <cjson/cJSON.h>
#include <argon2.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h> // Pour srand
#include "../repository/user_repository.h"

extern PGconn* DB;

static void send_json(struct http_request* req, cJSON* j, int code) {
    char* txt = cJSON_PrintUnformatted(j);
    http_response(req, code, txt, strlen(txt));
    free(txt);
    cJSON_Delete(j);
}

// Fonction pour générer un sel aléatoire
static void generate_salt(uint8_t *salt, size_t len) {
    // Note: pour une vraie application, utiliser une source d'entropie plus forte
    // comme /dev/urandom ou la Crypto API du système.
    for (size_t i = 0; i < len; i++) {
        salt[i] = rand();
    }
}

int auth_register(struct http_request* req) {
    size_t len;
    const uint8_t *body = http_body_read(req, &len);
    cJSON* j = cJSON_ParseWithLength((const char*)body, len);
    if (!j) { http_response(req, 400, NULL, 0); return KORE_RESULT_OK; }

    const char *n = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
    const char *e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char *p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    if (!n || !e || !p) {
        cJSON_Delete(j);
        http_response(req, 400, NULL, 0);
        return KORE_RESULT_OK;
    }

    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));

    char encoded_hash[128];
    if (argon2id_hash_encoded(2, 1 << 16, 1, p, strlen(p), salt, sizeof(salt), encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
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
    size_t len;
    const uint8_t *body = http_body_read(req, &len);
    cJSON* j = cJSON_ParseWithLength((const char*)body, len);
    if (!j) { http_response(req, 400, NULL, 0); return KORE_RESULT_OK; }

    const char* e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char* p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));
    cJSON_Delete(j);

    if (!e || !p) { http_response(req, 400, NULL, 0); return KORE_RESULT_OK; }

    User u;
    if (!user_repo_find_by_mail(DB, e, &u)) {
        http_response(req, 401, NULL, 0);
        return KORE_RESULT_OK;
    }

    if (argon2id_verify(u.password_hash, p, strlen(p)) != ARGON2_OK) {
        http_response(req, 401, NULL, 0);
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
    const char *hdr = http_request_header(req, "cookie");
    if (!hdr) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }

    const char* jwt_val = strstr(hdr, "jwt=");
    if (!jwt_val) { http_response(req, 401, NULL, 0); return KORE_RESULT_OK; }

    int uid = atoi(jwt_val + 4);
    User u;
    if (!user_repo_find(DB, uid, &u)) {
        http_response(req, 401, NULL, 0);
        return KORE_RESULT_OK;
    }

    cJSON* o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    cJSON_AddStringToObject(o, "name", u.name);
    cJSON_AddNumberToObject(o, "id", u.id);
    send_json(req, o, 200);
    return KORE_RESULT_OK;
}
