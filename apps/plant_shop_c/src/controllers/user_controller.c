#include "user_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <argon2.h>
#include "../repository/user_repository.h"

extern PGconn* DB;

static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
    char *text = cJSON_PrintUnformatted(j);
    mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
    free(text);
    if (j) cJSON_Delete(j);
}

static int get_current_user_id(struct mg_http_message* hm) {
    struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (!cookie_hdr) {
        printf("❌ get_current_user_id : aucun header Cookie reçu.\n");
        return 0;
    }
    printf("🔎 get_current_user_id : header Cookie reçu : '%.*s'\n", (int)cookie_hdr->len, cookie_hdr->buf);

    char jwt_val_str[32];
    int jwt_found = mg_http_get_var(cookie_hdr, "jwt", jwt_val_str, sizeof(jwt_val_str));
    if (jwt_found <= 0) {
        printf("❌ get_current_user_id : cookie 'jwt' absent ou invalide.\n");
        return 0;
    }
    printf("🔎 get_current_user_id : valeur brute du jwt : '%s'\n", jwt_val_str);

    int uid = atoi(jwt_val_str);
    if (uid <= 0) {
        printf("❌ get_current_user_id : uid extrait <= 0 (%d), cookie corrompu ou absent.\n", uid);
        return 0;
    }
    printf("✅ get_current_user_id : id utilisateur extrait : %d\n", uid);
    return uid;
}

static int is_admin(struct mg_http_message* hm) {
    int uid = get_current_user_id(hm);
    if (uid == 0) return 0;
    return user_repo_is_admin(DB, uid);
}

static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = (uint8_t)rand();
    }
}

void user_create(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    const char *name_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "name"));
    const char *email_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "email"));
    const char *pwd_str = cJSON_GetStringValue(cJSON_GetObjectItem(j, "password"));

    if (!name_str || !email_str || !pwd_str) {
        cJSON_Delete(j);
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Missing fields\"}");
        return;
    }

    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));
    char encoded_hash[128];
    if (argon2id_hash_encoded(2, 1 << 16, 1, pwd_str, strlen(pwd_str), salt, sizeof(salt), 32, encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
        cJSON_Delete(j);
        mg_http_reply(c, 500, "Content-Type: application/json\r\n", "{\"error\":\"Hashing failed\"}");
        return;
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
    send_json_reply(c, o, 201);
}

void user_get(struct mg_connection* c, struct mg_http_message *hm, int id) {
    printf("🟦 user_get : requête reçue pour id = %d\n", id);

    User u;
    if (!user_repo_find(DB, id, &u)) {
        printf("❌ user_get : utilisateur id=%d non trouvé\n", id);
        mg_http_reply(c, 404, "", "");
        return;
    }
    printf("✅ user_get : utilisateur id=%d trouvé, nom=%s, email=%s, admin=%d\n", u.id, u.name, u.email, u.is_admin);

    cJSON* j = cJSON_CreateObject();
    cJSON_AddNumberToObject(j, "id", u.id);
    cJSON_AddStringToObject(j, "name", u.name);
    cJSON_AddStringToObject(j, "email", u.email);
    cJSON_AddBoolToObject(j, "admin", u.is_admin);

    printf("🚀 user_get : envoi de la réponse JSON pour id=%d\n", u.id);
    send_json_reply(c, j, 200);
    (void)hm;
}

void user_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
    printf("[USER CONTROLLER] Réception de la requête PATCH /api/users/%d\n", id);
    printf("[USER CONTROLLER] Corps reçu : \"%.*s\"\n", (int)hm->body.len, hm->body.buf);

    int current_user_id = get_current_user_id(hm);
    printf("[USER CONTROLLER] ID user courant extrait du cookie : %d\n", current_user_id);

    int current_user_is_admin = user_repo_is_admin(DB, current_user_id);
    printf("[USER CONTROLLER] Statut admin du user courant : %d\n", current_user_is_admin);

    if (current_user_id != id && !current_user_is_admin) {
        printf("[USER CONTROLLER] Refus : ni admin ni modification de son propre profil (id cible = %d, id courant = %d)\n", id, current_user_id);
        mg_http_reply(c, 403, "", "");
        return;
    }

    cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
    if (!j) {
        printf("[USER CONTROLLER] JSON reçu invalide, abandon.\n");
        mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    if (!current_user_is_admin && cJSON_HasObjectItem(j, "admin")) {
        printf("[USER CONTROLLER] Tentative de modification du champ 'admin' refusée (non admin).\n");
        cJSON_DeleteItemFromObject(j, "admin");
    }

    printf("[USER CONTROLLER] Mise à jour du profil utilisateur %d.\n", id);
    user_repo_patch(DB, id, j);
    cJSON_Delete(j);
    printf("[USER CONTROLLER] Modification réussie (id = %d).\n", id);
    mg_http_reply(c, 200, "", "");
}

void user_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    user_repo_del(DB, id);
    mg_http_reply(c, 200, "", "");
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

void admin_users_list(struct mg_connection* c, struct mg_http_message *hm) {
    if (!is_admin(hm)) {
        mg_http_reply(c, 403, "", "");
        return;
    }
    cJSON* arr = cJSON_CreateArray();
    user_repo_each(DB, admin_users_list_cb, arr);
    send_json_reply(c, arr, 200);
}
