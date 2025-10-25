#ifndef CTRL_AUTH_H
#define CTRL_AUTH_H
#include <kore/http.h>
int auth_login(struct http_request*);
int auth_register(struct http_request*);
int auth_me(struct http_request*);
#endif
