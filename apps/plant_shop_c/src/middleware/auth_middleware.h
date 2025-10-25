#ifndef AUTH_MIDDLEWARE_H
#define AUTH_MIDDLEWARE_H

#include "../mongoose/mongoose.h"

// Middleware : vérifie le JWT et extrait user_id
void require_auth(struct mg_connection* c, struct mg_http_message* hm,
                  void (*next)(struct mg_connection*, struct mg_http_message*, int),
                  int resource_id);

// Middleware : vérifie JWT + rôle admin
void require_admin(struct mg_connection* c, struct mg_http_message* hm,
                   void (*next)(struct mg_connection*, struct mg_http_message*, int),
                   int resource_id);

#endif
