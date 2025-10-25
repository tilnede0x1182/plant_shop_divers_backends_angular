#include "user_controller.h"
#include <kore/kore.h>
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <argon2.h>
#include "../repository/user_repository.h"

extern PGconn* DB;

// Fonction utilitaire pour lire le corps de la requête dans un buffer
static int read_body_to_buf(struct http_request *req, struct kore_buf *buf) {
    size_t len;
    ssize_t ret;
    u_int8_t tmp[1024];

    if (http_body_rewind(req) != KORE_RESULT_OK) {
        return KORE_RESULT_ERROR;
    }

    for (;;) {
        ret = http_body_read(req, tmp, sizeof(tmp));
        if (ret == -1) {
            kore_log(LOG_ERR, "failed to read http body");
            return KORE_RESULT_ERROR;
        }
        if (ret == 0) {
            break;
        }
        kore_buf_append(buf, tmp, ret);
    }
    return KORE_RESULT_OK;
}

static void jout(struct http_request* r, cJSON* j, int c) {
    char *t = cJSON_PrintUnformatted(j);
    http_response(r, c, t, strlen(t));
    free(t);
    cJSON_Delete(j);
}

static int get_current_user_id(struct http_request* req) {
    const char* ck = NULL;
    if (!http_request_header(req, "cookie", &ck) || !ck) {
        return 0;
    }
    const char* jwt_val = strstr(ck, "jwt=");
    if (!jwt_val) return 0;
    return atoi(jwt_val + 4);
}

static int is_admin(struct http_request* req) {
    int uid = get_current_user_id(req);
    if (uid == 0) return 0;
    return user_repo_is_admin(DB, uid);
}

static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = rand();
    }
}

int user_create(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }

    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    const char *name_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
    const char *email_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char *pwd_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    if (!name_str || !email_str || !pwd_str) {
        cJSON_Delete(j);
        http_response(req, 400, "Missing fields", 14);
        return KORE_RESULT_OK;
    }

    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));
    char encoded_hash[128];
    if (argon2id_hash_encoded(2, 1 << 16, 1, pwd_str, strlen(pwd_str), salt, sizeof(salt), 32, encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
        cJSON_Delete(j);
        http_response(req, 500, "Hashing failed", 14);
        return KORE_RESULT_OK;
    }

    User u = {0};
    snprintf(u.name, sizeof(u.name), "%s", name_str);
    snprintf(u.email, sizeof(u.email), "%s", email_str);
    snprintf(u.password_hash, sizeof(u.password_hash), "%s", encoded_hash);
    u.is_admin = cJSON_IsTrue(cJSON_GetObjectItem(j, "admin"));

    u.id = user_repo_add(DB, &u);
    cJSON_Delete(j);

    cJSON *o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", u.id);
    jout(req, o, 201);
    return KORE_RESULT_OK;
}

int user_get(struct http_request* req) {
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    User u;
    if (!user_repo_find(DB, id, &u)) { http_response(req, 404, NULL, 0); return KORE_RESULT_OK; }

    cJSON* j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", u.id);
    cJSON_AddStringToObject(j, "name", u.name);
    cJSON_AddStringToObject(j, "email", u.email);
    cJSON_AddBoolToObject(j, "admin", u.is_admin);
    jout(req, j, 200);
    return KORE_RESULT_OK;
}

int user_patch(struct http_request* req) {
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    int current_user_id = get_current_user_id(req);
    int current_user_is_admin = user_repo_is_admin(DB, current_user_id);

    if (current_user_id != id && !current_user_is_admin) {
        http_response(req, 403, NULL, 0);
        return KORE_RESULT_OK;
    }

    struct kore_buf *buf = kore_buf_alloc(0);
    if (read_body_to_buf(req, buf) != KORE_RESULT_OK) {
        kore_buf_free(buf);
        http_response(req, 500, "Cannot read body", 15);
        return KORE_RESULT_OK;
    }
    cJSON* j = cJSON_ParseWithLength((const char*)buf->data, buf->offset);
    kore_buf_free(buf);

    if (!j) { http_response(req, 400, "Invalid JSON", 12); return KORE_RESULT_OK; }

    // Un utilisateur ne peut pas changer son propre statut admin. Seul un admin peut le faire.
    if (!current_user_is_admin) {
        if (cJSON_HasObjectItem(j, "admin")) {
            cJSON_DeleteItemFromObject(j, "admin");
        }
    }

    user_repo_patch(DB, id, j);
    cJSON_Delete(j);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}

int user_del(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    int id;
    http_populate_get(req);
    if (!http_argument_get_int32(req, "id", &id)) {
        http_response(req, 400, "Invalid ID", 10);
        return KORE_RESULT_OK;
    }
    user_repo_del(DB, id);
    http_response(req, 200, NULL, 0);
    return KORE_RESULT_OK;
}

static void admin_users_list_cb(User* u, void* arg) {
    cJSON* arr = (cJSON*)arg;
    cJSON* j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", u->id);
    cJSON_AddStringToObject(j, "email", u->email);
    cJSON_AddStringToObject(j, "name", u->name);
    cJSON_AddBoolToObject(j, "admin", u->is_admin);
    cJSON_AddItemToArray(arr, j);
}

int admin_users_list(struct http_request* req) {
    if (!is_admin(req)) { http_response(req, 403, NULL, 0); return KORE_RESULT_OK; }
    cJSON* arr = cJSON_CreateArray();
    user_repo_each(DB, admin_users_list_cb, arr);
    jout(req, arr, 200);
    return KORE_RESULT_OK;
}
