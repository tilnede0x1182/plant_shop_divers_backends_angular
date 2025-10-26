#ifndef REPO_ORDER_H
#define REPO_ORDER_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/order.h"

int  order_repo_add(PGconn*, int user_id, cJSON* items);
cJSON* order_repo_list(PGconn*, int user_id);
void order_repo_patch(PGconn*, int id, cJSON* data);
void order_repo_del(PGconn*, int id);
int  order_repo_belongs_to(PGconn*, int order_id, int user_id);
int  order_repo_is_admin(PGconn *db, int user_id);
int order_repo_update_status(PGconn *conn, int order_id, const char* status);

#endif
