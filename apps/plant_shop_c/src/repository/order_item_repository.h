#ifndef REPO_ORDER_ITEM_H
#define REPO_ORDER_ITEM_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/order_item.h"

void order_item_repo_add(PGconn*, const OrderItem*);
void order_item_repo_by_order(PGconn*, int, void(*)(OrderItem*, void*), void*);
void order_item_repo_patch(PGconn*, int id, cJSON* data);
void order_item_repo_del(PGconn*, int id);

#endif
