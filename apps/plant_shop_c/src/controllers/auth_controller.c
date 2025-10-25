#include "auth_controller.h"
#include <cjson/cJSON.h>
#include <argon2.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include "../repository/user_repository.h"

extern PGconn* DB;

static void send_json(struct mg_connection* c, cJSON* j, int code) {
    char* txt = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", txt);
    free(txt);
    if (j) cJSON_Delete(j);
}

static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = (uint8_t)rand();
    }
}

void auth_register(struct mg_connection* c, struct mg_http_message *hm) {
    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "", "{\"error\":\"Invalid JSON\"}\n");
        return;
    }

    const char *n = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
    const char *e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char *p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    if (!n || !e || !p) {
        cJSON_Delete(j);
        mg_http_reply(c, 400, "", "{\"error\":\"Missing fields\"}\n");
        return;
    }

    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));

    char encoded_hash[128];
    if (argon2id_hash_encoded(2, 1 << 16, 1, p, strlen(p), salt, sizeof(salt), 32, encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
        cJSON_Delete(j);
        mg_http_reply(c, 500, "", "{\"error\":\"Hashing failed\"}\n");
        return;
    }

    User u = {.is_admin = 0};
    snprintf(u.name, sizeof(u.name), "%s", n);
    snprintf(u.email, sizeof(u.email), "%s", e);
    snprintf(u.password_hash, sizeof(u.password_hash), "%s", encoded_hash);

    u.id = user_repo_add(DB, &u);
    cJSON_Delete(j);

    if (u.id == 0) {
        mg_http_reply(c, 409, "Content-Type: application/json\r\n", "{\"error\":\"Email already exists\"}\n");
        return;
    }

    cJSON *out = cJSON_CreateObject();
    cJSON_AddNumberToObject(out, "id", u.id);
    send_json(c, out, 201);
}

void auth_login(struct mg_connection* c, struct mg_http_message *hm) {
    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}\n");
        return;
    }

    const char* e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char* p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));
    cJSON_Delete(j);

    if (!e || !p) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing fields\"}\n");
        return;
    }

    User u;
    if (!user_repo_find_by_mail(DB, e, &u)) {
        mg_http_reply(c, 401, "Content-Type: application/json\r\n", "{\"error\":\"Invalid credentials\"}\n");
        return;
    }

    if (argon2id_verify(u.password_hash, p, strlen(p)) != ARGON2_OK) {
        mg_http_reply(c, 401, "Content-Type: application/json\r\n", "{\"error\":\"Invalid credentials\"}\n");
        return;
    }

    char cookie[256];
    snprintf(cookie, sizeof(cookie), "Set-Cookie: jwt=%d; Path=/; HttpOnly; Max-Age=86400", u.id);

    cJSON *o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    char* txt = cJSON_PrintUnformatted(o);
    mg_http_reply(c, 201, cookie, "Content-Type: application/json\r\n%s", txt);
    free(txt);
    cJSON_Delete(o);
}

void auth_me(struct mg_connection* c, struct mg_http_message *hm) {
    struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (!cookie_hdr) {
        mg_http_reply(c, 401, "", "{\"error\":\"Unauthorized\"}\n");
        return;
    }

    char jwt_val_str[32];
    if (mg_http_get_var(cookie_hdr, "jwt", jwt_val_str, sizeof(jwt_val_str)) <= 0) {
        mg_http_reply(c, 401, "", "{\"error\":\"Unauthorized\"}\n");
        return;
    }

    int uid = atoi(jwt_val_str);
    if (uid == 0) {
        mg_http_reply(c, 401, "", "{\"error\":\"Invalid token\"}\n");
        return;
    }

    User u;
    if (!user_repo_find(DB, uid, &u)) {
        mg_http_reply(c, 401, "", "{\"error\":\"User not found\"}\n");
        return;
    }

    cJSON* o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    cJSON_AddStringToObject(o, "name", u.name);
    cJSON_AddNumberToObject(o, "id", u.id);
    send_json(c, o, 200);
}
