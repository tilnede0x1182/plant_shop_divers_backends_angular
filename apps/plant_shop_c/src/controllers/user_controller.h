#ifndef CTRL_USER_H
#define CTRL_USER_H

#include "mongoose/mongoose.h"

void user_create(struct mg_connection *c, struct mg_http_message *hm);
void user_get(struct mg_connection *c, struct mg_http_message *hm, int id);
void user_patch(struct mg_connection *c, struct mg_http_message *hm, int id);
void user_del(struct mg_connection *c, struct mg_http_message *hm, int id);
void admin_users_list(struct mg_connection *c, struct mg_http_message *hm);

#endif
