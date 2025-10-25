#ifndef CTRL_ORDER_H
#define CTRL_ORDER_H
#include <regex.h>
#include <kore/http.h>
int orders_list(struct http_request*);
int orders_create(struct http_request*);
int orders_patch(struct http_request*);
int orders_del(struct http_request*);
#endif
