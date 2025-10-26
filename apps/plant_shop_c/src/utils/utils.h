#ifndef UTILS_H
#define UTILS_H

#include <stddef.h>
#include "../mongoose/mongoose.h"

void read_db_env(char* url, char* user, char* pass);
void read_server_env(char* port, char* jwt_secret);
int get_cookie_manual(struct mg_http_message* hm, const char* name, char* out, size_t sz);
int get_current_user_id(struct mg_http_message *hm);

#endif
