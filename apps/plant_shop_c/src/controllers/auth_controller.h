#ifndef CTRL_AUTH_H
#define CTRL_AUTH_H

#include "mongoose/mongoose.h"

void auth_login(struct mg_connection *c, struct mg_http_message *hm);
void auth_register(struct mg_connection *c, struct mg_http_message *hm);
void auth_me(struct mg_connection *c, struct mg_http_message *hm);

#endif
