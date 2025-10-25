#ifndef ROUTES_H
#define ROUTES_H

#include "mongoose/mongoose.h"

/** Teste hm->uri sur un motif glob simple. */
static inline bool mg_http_match_uri(const struct mg_http_message *hm,
									 const char *pattern) {
	return mg_match(hm->uri, mg_str(pattern), NULL);
}

void route_request(struct mg_connection *c, struct mg_http_message *hm);

#endif // ROUTES_H
