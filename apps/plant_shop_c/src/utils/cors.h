#ifndef CORS_H
#define CORS_H

#include "../mongoose/mongoose.h"
#include <stdarg.h>

// Envoie une réponse HTTP avec headers CORS
void cors_reply(struct mg_connection *c, int status, const char *extra_headers, const char *fmt, ...);

// Envoie une réponse JSON avec headers CORS
void cors_reply_json(struct mg_connection *c, int status, const char *json);

// Gère les requêtes OPTIONS (preflight)
// Retourne 1 si c'est un preflight (déjà traité), 0 sinon
int cors_handle_preflight(struct mg_connection *c, struct mg_http_message *hm);

#endif
