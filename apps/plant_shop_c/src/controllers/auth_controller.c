#include "auth_controller.h"
#include <cjson/cJSON.h>
#include <argon2.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include "../repository/user_repository.h"
#include "../utils/utils.h"

extern PGconn* DB;

/**
 * Envoie une réponse JSON au client.
 *
 * @param c Connexion Mongoose
 * @param j Objet JSON à envoyer
 * @param code Code HTTP de réponse
 */
static void send_json(struct mg_connection* c, cJSON* j, int code) {
    char* txt = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", txt);
    free(txt);
    if (j) cJSON_Delete(j);
}

/**
 * Génère un sel aléatoire pour le hachage.
 *
 * @param salt Buffer pour stocker le sel
 * @param len Longueur du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = (uint8_t)rand();
    }
}

/**
 * Gère l inscription d un nouvel utilisateur.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
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

/**
 * Gère la connexion d un utilisateur.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void auth_login(struct mg_connection* c, struct mg_http_message *hm) {
    struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (cookie_hdr) {
        // printf("[LOGIN] Cookie: \"%.*s\"\n",
              //  (int)cookie_hdr->len, cookie_hdr->buf);
    } else {
        // printf("[LOGIN] No Cookie\n");
    }

    // printf("[LOGIN] Body: \"%.*s\"\n",
          //  (int)hm->body.len, hm->body.buf);

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        // printf("[LOGIN] Invalid JSON\n");
        mg_http_reply(c, 400,
                      "Content-Type: application/json\r\n",
                      "{\"error\":\"Invalid JSON\"}\n");
        return;
    }

    const char* e = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char* p = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    char email_buf[128]    = {0};
    char password_buf[128] = {0};
    if (e) snprintf(email_buf, sizeof(email_buf), "%s", e);
    if (p) snprintf(password_buf, sizeof(password_buf), "%s", p);

    // printf("[LOGIN] Email: '%s', Password: '%s'\n",
          //  email_buf, password_buf);

    cJSON_Delete(j);

    if (!e || !p) {
        // printf("[LOGIN] Missing fields\n");
        mg_http_reply(c, 400,
                      "Content-Type: application/json\r\n",
                      "{\"error\":\"Missing fields\"}\n");
        return;
    }

    User u;
    if (!user_repo_find_by_mail(DB, email_buf, &u)) {
        // printf("[LOGIN] User not found\n");
        mg_http_reply(c, 401,
                      "Content-Type: application/json\r\n",
                      "{\"error\":\"Invalid credentials\"}\n");
        return;
    }
    // printf("[LOGIN] Found user id=%d\n", u.id);

    int verify_result = argon2id_verify(u.password_hash,
                                        password_buf,
                                        strlen(password_buf));
    // printf("[LOGIN] Password verify result: %d\n", verify_result);
    if (verify_result != ARGON2_OK) {
        // printf("[LOGIN] Incorrect password\n");
        mg_http_reply(c, 401,
                      "Content-Type: application/json\r\n",
                      "{\"error\":\"Invalid credentials\"}\n");
        return;
    }

    char cookie[256];
    snprintf(cookie, sizeof(cookie),
             "Set-Cookie: plant_shop_c_backend=%d; Path=/; HttpOnly; Max-Age=86400",
             u.id);
    // printf("[LOGIN] Set-Cookie: %s\n", cookie);

    char headers[512];
    snprintf(headers, sizeof(headers),
             "%s\r\nContent-Type: application/json\r\n",
             cookie);

    cJSON *o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    char* txt = cJSON_PrintUnformatted(o);

    mg_http_reply(c, 201,
                  headers,
                  "%s", txt);

    free(txt);
    cJSON_Delete(o);
}

/**
 * Retourne les informations de l utilisateur connecté.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void auth_me(struct mg_connection* c, struct mg_http_message *hm) {
    char jwt_val_str[32] = {0};
    if (!get_cookie_manual(hm, "plant_shop_c_backend", jwt_val_str, sizeof(jwt_val_str))) {
        mg_http_reply(c, 401, "Content-Type: application/json\r\n",
                      "{\"error\":\"Unauthorized\"}\n");
        return;
    }

    int uid = atoi(jwt_val_str);
    if (uid == 0) {
        mg_http_reply(c, 401, "Content-Type: application/json\r\n",
                      "{\"error\":\"Invalid token\"}\n");
        return;
    }

    User u;
    if (!user_repo_find(DB, uid, &u)) {
        // printf("[auth_me] No user with id %d\n", uid);
        mg_http_reply(c, 401, "Content-Type: application/json\r\n",
                      "{\"error\":\"User not found\"}\n");
        return;
    }

    cJSON* o = cJSON_CreateObject();
    cJSON_AddStringToObject(o, "email", u.email);
    cJSON_AddStringToObject(o, "name",  u.name);
    cJSON_AddNumberToObject(o, "id",    u.id);
    cJSON_AddBoolToObject(o,   "admin", u.is_admin);
    send_json(c, o, 200);
}

/**
 * Déconnecte l utilisateur en supprimant le cookie.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void auth_logout(struct mg_connection* c, struct mg_http_message *hm) {
    mg_http_reply(c, 200, "Set-Cookie: plant_shop_c_backend=; Path=/; HttpOnly; Max-Age=0\r\n", "");
}
