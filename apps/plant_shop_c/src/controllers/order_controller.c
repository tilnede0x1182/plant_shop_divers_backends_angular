#include "order_controller.h"
#include <stdint.h>
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdio.h>

extern PGconn* DB;

static int is_admin(struct mg_http_message* hm);

void patch_order_status(struct mg_connection* c, struct mg_http_message* hm, int order_id) {
	if (!is_admin(hm)) {
			mg_http_reply(c, 403, "Content-Type: application/json\r\n", "{\"error\":\"Accès interdit\"}\n");
			return;
	}
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) {
		mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}\n");
		return;
	}
	const char* new_status = cJSON_GetStringValue(cJSON_GetObjectItem(json, "status"));
	if (!new_status) {
		cJSON_Delete(json);
		mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing status\"}\n");
		return;
	}
	if (!order_repo_update_status(DB, order_id, new_status)) {
		cJSON_Delete(json);
		mg_http_reply(c, 500, "Content-Type: application/json\r\n", "{\"error\":\"Update failed\"}\n");
		return;
	}
	cJSON_Delete(json);
	mg_http_reply(c, 200, "Content-Type: application/json\r\n", "{\"status\":\"%s\"}\n", new_status);
}

static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
    char *text = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
    free(text);
    if (j) cJSON_Delete(j);
}

static int get_current_user_id(struct mg_http_message* hm) {
    struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (!cookie_hdr) return 0;
    char jwt_val_str[32];
    if (mg_http_get_var(cookie_hdr, "jwt", jwt_val_str, sizeof(jwt_val_str)) <= 0) return 0;
    return atoi(jwt_val_str);
}

static int is_admin(struct mg_http_message* hm) {
    int uid = get_current_user_id(hm);
    return user_repo_is_admin(DB, uid);
}

void orders_create(struct mg_connection* c, struct mg_http_message *hm) {
    int uid = get_current_user_id(hm);
    if (!uid) {
        mg_http_reply(c, 401, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    cJSON *items = cJSON_GetObjectItem(j, "items");
    if (!items || !cJSON_IsArray(items)) {
        cJSON_Delete(j);
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing 'items' array\"}");
        return;
    }

    int oid = order_repo_add(DB, uid, items);
    cJSON_Delete(j);
    cJSON* o = cJSON_CreateObject();
    cJSON_AddNumberToObject(o, "id", oid);
    send_json_reply(c, o, 201);
}

void orders_list(struct mg_connection* c, struct mg_http_message *hm) {
    int uid = get_current_user_id(hm);
    if (!uid) {
        mg_http_reply(c, 401, "", "");
        return;
    }
    cJSON* arr = order_repo_list(DB, uid);
    send_json_reply(c, arr, 200);
}

void orders_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
	fprintf(stderr, "[DEBUG][PATCH] appel /api/orders/%d\n", id);

	if (!is_admin(hm)) {
		fprintf(stderr, "[DEBUG][PATCH] refus : utilisateur non-admin\n");
		mg_http_reply(c, 403, "", "");
		return;
	}

	cJSON *j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!j) {
		fprintf(stderr, "[DEBUG][PATCH] JSON invalide\n");
		mg_http_reply(c, 400,
		              "Content-Type: application/json\r\n",
		              "{\"error\":\"Invalid JSON\"}");
		return;
	}

	order_repo_patch(DB, id, j);
	cJSON_Delete(j);

	fprintf(stderr, "[DEBUG][PATCH] ordre %d traité\n", id);
	mg_http_reply(c, 200, "", "");
}

void orders_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    order_repo_del(DB, id);
    mg_http_reply(c, 200, "", "");
}
