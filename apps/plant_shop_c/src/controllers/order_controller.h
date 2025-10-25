#ifndef CTRL_ORDER_H
#define CTRL_ORDER_H

#include "mongoose/mongoose.h"

void orders_list(struct mg_connection *c, struct mg_http_message *hm);
void orders_create(struct mg_connection *c, struct mg_http_message *hm);
void orders_patch(struct mg_connection *c, struct mg_http_message *hm, int id);
void orders_del(struct mg_connection *c, struct mg_http_message *hm, int id);

#endif
