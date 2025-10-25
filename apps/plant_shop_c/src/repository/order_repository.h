#ifndef REPO_ORDER_H
#define REPO_ORDER_H
#include <libpq-fe.h>
#include "../models/order.h"

int  order_repo_insert(PGconn*,const Order*);
int  order_repo_find(PGconn*,int,Order*);
void order_repo_update_status(PGconn*,int,const char*);
void order_repo_update_total(PGconn*,int,int);
void order_repo_delete(PGconn*,int);
void order_repo_all_by_user(PGconn*,int,void(*)(Order*,void*),void*);
#endif
