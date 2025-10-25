#ifndef JWT_H
#define JWT_H

#include <stdbool.h>
#include <stddef.h>

struct mg_http_message;

bool jwt_generate_token(int user_id, const char* email,
                        char* token_out, size_t token_size);

bool jwt_verify_token(const char* token,
                      int* user_id_out,
                      char* email_out,
                      size_t email_size);

int  extract_user_id_from_cookie(struct mg_http_message* hm);

#endif  /* JWT_H */
