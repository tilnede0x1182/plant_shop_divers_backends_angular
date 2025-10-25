#ifndef REPO_ORDER_ITEM_H
#define REPO_ORDER_ITEM_H
#include <libpq-fe.h>
#include "../models/order_item.h"

void order_item_repo_add(PGconn*,const OrderItem*);
void order_item_repo_by_order(PGconn*,int,void(*)(OrderItem*,void*),void*);
#endif
