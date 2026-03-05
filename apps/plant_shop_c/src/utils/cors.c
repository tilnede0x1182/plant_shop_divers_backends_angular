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
 * @param header_buffer Buffer de destination
 * @param header_buffer_size Taille du buffer
 * @param extra_headers En-têtes supplémentaires (peut être NULL)
 */
static void build_cors_headers(char *header_buffer, size_t header_buffer_size, const char *extra_headers) {
	snprintf(header_buffer, header_buffer_size, "%s%s", CORS_HEADERS, extra_headers ? extra_headers : "");
}

/**
 * Alloue et formate le corps de la réponse.
 *
 * @param format Format printf
 * @param arg_list Liste d arguments variadiques
 * @return Buffer alloué ou NULL si erreur
 */
static char* format_body(const char *format, va_list arg_list) {
	va_list arg_list_copy;
	va_copy(arg_list_copy, arg_list);
	int needed = vsnprintf(NULL, 0, format, arg_list_copy) + 1;
	va_end(arg_list_copy);
	char *body = malloc(needed);
	if (body) vsnprintf(body, needed, format, arg_list);
	return body;
}

/**
 * Envoie une réponse HTTP avec les en-têtes CORS.
 *
 * @param mongoose_connection Connexion mongoose
 * @param status Code HTTP de réponse
 * @param extra_headers En-têtes supplémentaires (peut être NULL)
 * @param format Format printf pour le corps de réponse
 * @param ... Arguments variadiques pour le format
 */
void cors_reply(struct mg_connection *mongoose_connection, int status, const char *extra_headers, const char *format, ...) {
	char headers[1024];
	build_cors_headers(headers, sizeof(headers), extra_headers);
	va_list arg_list;
	va_start(arg_list, format);
	char *body = format_body(format, arg_list);
	va_end(arg_list);
	if (!body) { mg_http_reply(mongoose_connection, 500, headers, "{\"error\":\"Memory allocation failed\"}"); return; }
	mg_http_reply(mongoose_connection, status, headers, "%s", body);
	free(body);
}

/**
 * Envoie une réponse JSON avec les en-têtes CORS.
 *
 * @param mongoose_connection Connexion mongoose
 * @param status Code HTTP de réponse
 * @param json Chaîne JSON à envoyer
 */
void cors_reply_json(struct mg_connection *mongoose_connection, int status, const char *json) {
	cors_reply(mongoose_connection, status, "Content-Type: application/json\r\n", "%s", json);
}

/**
 * Gère les requêtes CORS preflight (OPTIONS).
 *
 * @param mongoose_connection Connexion mongoose
 * @param http_message Message HTTP de la requête
 * @return 1 si requête OPTIONS traitée, 0 sinon
 */
int cors_handle_preflight(struct mg_connection *mongoose_connection, struct mg_http_message *http_message) {
	if (mg_strcmp(http_message->method, mg_str("OPTIONS")) == 0) {
		cors_reply(mongoose_connection, 204, "", "");
		return 1;
	}
	return 0;
}
