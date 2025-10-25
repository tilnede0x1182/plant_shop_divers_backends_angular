#ifndef CTRL_USER_H
#define CTRL_USER_H
#include <kore/http.h>
int user_create(struct http_request*);
int user_get(struct http_request*);
int user_patch(struct http_request*);
int user_del(struct http_request*);
int admin_users_list(struct http_request*);
#endif
