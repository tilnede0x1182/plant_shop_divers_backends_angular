#ifndef REPO_USER_H
#define REPO_USER_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/user.h"

int  user_repo_add(PGconn *conn, const User *u);
int  user_repo_find(PGconn *conn, int id, User *out);
int  user_repo_find_by_mail(PGconn *conn, const char *email, User *out);
void user_repo_patch(PGconn *conn, int id, cJSON *patch_data);
void user_repo_del(PGconn *conn, int id);
void user_repo_each(PGconn *conn, void (*cb)(User*, void*), void *ctx);
int  user_repo_is_admin(PGconn *conn, int id);

#endif
