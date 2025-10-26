#include "plant_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/plant_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdint.h>
#include "../utils/utils.h"

extern PGconn* DB;

static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
    char *text = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
    free(text);
    if (j) cJSON_Delete(j);
}

// static int get_current_user_id(struct mg_http_message* hm) {
//     struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
//     if (!cookie_hdr) return 0;
//     char jwt_val_str[32];
//     if (mg_http_get_var(cookie_hdr, "plant_shop_c_backend", jwt_val_str, sizeof(jwt_val_str)) <= 0) return 0;
//     return atoi(jwt_val_str);
// }

static int is_admin(struct mg_http_message* hm) {
    int uid = get_current_user_id(hm);
    return user_repo_is_admin(DB, uid);
}

void plant_get(struct mg_connection* c, struct mg_http_message *hm, int id) {
    Plant p;
    if (!plant_repo_find(DB, id, &p)) {
        mg_http_reply(c, 404, "", "");
        return;
    }
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", p.id);
    cJSON_AddStringToObject(j, "name", p.name);
    cJSON_AddStringToObject(j, "description", p.description);
    cJSON_AddNumberToObject(j, "price", p.price);
    cJSON_AddNumberToObject(j, "stock", p.stock);
    send_json_reply(c, j, 200);
    (void)hm;
}

static void admin_plants_list_cb(Plant* p, void* a) {
    cJSON *j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", p->id);
    cJSON_AddStringToObject(j, "name", p->name);
    cJSON_AddNumberToObject(j, "price", p->price);
    cJSON_AddNumberToObject(j, "stock", p->stock);
    cJSON_AddItemToArray((cJSON*)a, j);
}

void plants_list_public(struct mg_connection* c, struct mg_http_message *hm) {
    cJSON *arr = cJSON_CreateArray();
    plant_repo_each(DB, admin_plants_list_cb, arr);
    send_json_reply(c, arr, 200);
}

void admin_plants_list(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    cJSON *arr = cJSON_CreateArray();
    plant_repo_each(DB, admin_plants_list_cb, arr);
    send_json_reply(c, arr, 200);
}

void admin_plants_add(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

		Plant p = {0};
		const char *name = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
		const char *desc = cJSON_GetStringValue(cJSON_GetObjectItem(j, "description"));
		if (name) strncpy(p.name, name, sizeof(p.name) - 1);
		if (desc) strncpy(p.description, desc, sizeof(p.description) - 1);
		p.price = cJSON_GetObjectItem(j, "price")->valueint;
		p.stock = cJSON_GetObjectItem(j, "stock")->valueint;
		p.id = plant_repo_add(DB, &p);
    cJSON_Delete(j);

    cJSON *o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", p.id);
    send_json_reply(c, o, 201);
}

void admin_plants_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    plant_repo_patch(DB, id, j);
    cJSON_Delete(j);
    mg_http_reply(c, 200, "", "");
}

void admin_plants_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    plant_repo_del(DB, id);
    mg_http_reply(c, 200, "", "");
}
