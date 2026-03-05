/* ==============================================================================
   Importations
   ============================================================================== */
#include "cors.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/** En-têtes CORS standards */
static const char *CORS_HEADERS =
	"Access-Control-Allow-Origin: http://localhost:8300\r\n"
	"Access-Control-Allow-Credentials: true\r\n"
	"Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS\r\n"
	"Access-Control-Allow-Headers: Content-Type,Authorization,Cookie,X-Requested-With\r\n"
	"Access-Control-Expose-Headers: Set-Cookie\r\n";

/**
 * Construit les en-têtes complets avec CORS.
 *
 * @param buf Buffer de destination
 * @param buf_size Taille du buffer
 * @param extra_headers En-têtes supplémentaires (peut être NULL)
 */
static void build_cors_headers(char *buf, size_t buf_size, const char *extra_headers) {
	snprintf(buf, buf_size, "%s%s", CORS_HEADERS, extra_headers ? extra_headers : "");
}

/**
 * Alloue et formate le corps de la réponse.
 *
 * @param fmt Format printf
 * @param ap Liste d arguments variadiques
 * @return Buffer alloué ou NULL si erreur
 */
static char* format_body(const char *fmt, va_list ap) {
	va_list ap_copy;
	va_copy(ap_copy, ap);
	int needed = vsnprintf(NULL, 0, fmt, ap_copy) + 1;
	va_end(ap_copy);
	char *body = malloc(needed);
	if (body) vsnprintf(body, needed, fmt, ap);
	return body;
}

/**
 * Envoie une réponse HTTP avec les en-têtes CORS.
 *
 * @param conn Connexion mongoose
 * @param status Code HTTP de réponse
 * @param extra_headers En-têtes supplémentaires (peut être NULL)
 * @param fmt Format printf pour le corps de réponse
 * @param ... Arguments variadiques pour le format
 */
void cors_reply(struct mg_connection *conn, int status, const char *extra_headers, const char *fmt, ...) {
	char headers[1024];
	build_cors_headers(headers, sizeof(headers), extra_headers);
	va_list ap;
	va_start(ap, fmt);
	char *body = format_body(fmt, ap);
	va_end(ap);
	if (!body) { mg_http_reply(conn, 500, headers, "{\"error\":\"Memory allocation failed\"}"); return; }
	mg_http_reply(conn, status, headers, "%s", body);
	free(body);
}

/**
 * Envoie une réponse JSON avec les en-têtes CORS.
 *
 * @param conn Connexion mongoose
 * @param status Code HTTP de réponse
 * @param json Chaîne JSON à envoyer
 */
void cors_reply_json(struct mg_connection *conn, int status, const char *json) {
	cors_reply(conn, status, "Content-Type: application/json\r\n", "%s", json);
}

/**
 * Gère les requêtes CORS preflight (OPTIONS).
 *
 * @param conn Connexion mongoose
 * @param hm Message HTTP de la requête
 * @return 1 si requête OPTIONS traitée, 0 sinon
 */
int cors_handle_preflight(struct mg_connection *conn, struct mg_http_message *hm) {
	if (mg_strcmp(hm->method, mg_str("OPTIONS")) == 0) {
		cors_reply(conn, 204, "", "");
		return 1;
	}
	return 0;
}
