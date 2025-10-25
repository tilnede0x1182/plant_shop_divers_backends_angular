#include "cors.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void cors_reply(struct mg_connection *c, int status, const char *extra_headers, const char *fmt, ...) {
    // En-têtes CORS standards
    const char *cors_headers =
        "Access-Control-Allow-Origin: http://localhost:8300\r\n"
        "Access-Control-Allow-Credentials: true\r\n"
        "Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS\r\n"
        "Access-Control-Allow-Headers: Content-Type,Authorization,Cookie,X-Requested-With\r\n"
        "Access-Control-Expose-Headers: Set-Cookie\r\n";

    // Construction des headers complets
    char headers[1024];
    snprintf(headers, sizeof(headers), "%s%s", cors_headers, extra_headers ? extra_headers : "");

    va_list ap;
    va_start(ap, fmt);

    // Calculer la taille nécessaire
    va_list ap_copy;
    va_copy(ap_copy, ap);
    int needed = vsnprintf(NULL, 0, fmt, ap_copy) + 1;
    va_end(ap_copy);

    // Allouer dynamiquement
    char *body = malloc(needed);
    if (!body) {
        va_end(ap);
        fprintf(stderr, "❌ [CORS] Allocation mémoire échouée pour %d bytes\n", needed);
        mg_http_reply(c, 500, headers, "{\"error\":\"Memory allocation failed\"}");
        return;
    }

    vsnprintf(body, needed, fmt, ap);
    va_end(ap);

    // 🔍 LOG DÉTAILLÉ
    fprintf(stderr, "✅ [CORS] %d | Body size: %d bytes | Headers: %s\n",
            status, needed - 1, extra_headers ? extra_headers : "(none)");

    // Envoi de la réponse
    mg_http_reply(c, status, headers, "%s", body);
    free(body);
}

void cors_reply_json(struct mg_connection *c, int status, const char *json) {
    fprintf(stderr, "📤 [CORS_JSON] Envoi JSON %d | Taille: %zu bytes\n", status, strlen(json));
    cors_reply(c, status, "Content-Type: application/json\r\n", "%s", json);
}

int cors_handle_preflight(struct mg_connection *c, struct mg_http_message *hm) {
    if (mg_strcmp(hm->method, mg_str("OPTIONS")) == 0) {
        fprintf(stderr, "🔄 [PREFLIGHT] %.*s\n", (int)hm->uri.len, hm->uri.buf);
        cors_reply(c, 204, "", "");
        return 1;
    }
    return 0;
}
