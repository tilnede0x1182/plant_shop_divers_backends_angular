#ifndef CTRL_ORDER_ITEM_H
#define CTRL_ORDER_ITEM_H

#include "mongoose/mongoose.h"

void order_items_by_order(struct mg_connection *c, struct mg_http_message *hm, int order_id);
void order_item_patch(struct mg_connection *c, struct mg_http_message *hm, int id);
void order_item_del(struct mg_connection *c, struct mg_http_message *hm, int id);

#endif
