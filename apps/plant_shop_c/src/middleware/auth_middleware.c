#include "auth_middleware.h"
#include "../auth/jwt.h"
#include "../repository/user_repository.h"
#include "../utils/cors.h"
#include <stdio.h>
#include <string.h>

extern PGconn* DB;

// src/middleware/auth_middleware.c
static int extract_token(struct mg_http_message* hm, char* out, size_t out_size) {
    struct mg_str* auth_header = mg_http_get_header(hm, "Authorization");
    if (auth_header) {
        char auth[512];
        snprintf(auth, sizeof auth, "%.*s", (int)auth_header->len, auth_header->buf);
        if (strncmp(auth, "Bearer ", 7) == 0) {
            strncpy(out, auth + 7, out_size - 1);
            out[out_size - 1] = '\0';
            return 1;
        }
    }
    // Fallback : lire le cookie 'jwt'
    struct mg_str* cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (!cookie_hdr) return 0;
    if (mg_http_get_var(cookie_hdr, "jwt", out, out_size) <= 0) return 0;
    return 1;
}

// Envoie une erreur JSON
static void send_error(struct mg_connection* c, int code, const char* msg) {
    char json[256];
    snprintf(json, sizeof(json), "{\"error\":\"%s\"}", msg);
    cors_reply_json(c, code, json);
}

// Middleware : authentification simple (utilisateur connecté)
void require_auth(struct mg_connection* c, struct mg_http_message* hm,
                  void (*next)(struct mg_connection*, struct mg_http_message*, int),
                  int resource_id) {

    char token[512] = {0};
    if (!extract_token(hm, token, sizeof(token))) {
        send_error(c, 401, "Missing or invalid Authorization header");
        return;
    }

    int user_id = 0;
    char email[128] = {0};

    if (!jwt_verify_token(token, &user_id, email, sizeof(email))) {
        send_error(c, 401, "Invalid or expired token");
        return;
    }

    printf("✅ [AUTH] user_id=%d (%s) authentifié\n", user_id, email);
    next(c, hm, resource_id);
}

// Middleware : authentification + vérification rôle admin
void require_admin(struct mg_connection* c, struct mg_http_message* hm,
                   void (*next)(struct mg_connection*, struct mg_http_message*, int),
                   int resource_id) {

    char token[512] = {0};
    if (!extract_token(hm, token, sizeof(token))) {
        send_error(c, 401, "Missing or invalid Authorization header");
        return;
    }

    int user_id = 0;
    char email[128] = {0};

    if (!jwt_verify_token(token, &user_id, email, sizeof(email))) {
        send_error(c, 401, "Invalid or expired token");
        return;
    }

    int is_admin = user_repo_is_admin(DB, user_id);

    printf("[DEBUG][IS_ADMIN] user_id=%d (%s) | is_admin=%d\n", user_id, email, is_admin);

    if (!is_admin) {
        send_error(c, 403, "Forbidden");
        return;
    }

    printf("✅ [ADMIN] user_id=%d (%s) validé\n", user_id, email);
    next(c, hm, resource_id);
}
