/* ==============================================================================
   Importations
   ============================================================================== */
#include "user_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <argon2.h>
#include "../repository/user_repository.h"
#include "../utils/utils.h"

/* ==============================================================================
   Données
   ============================================================================== */
extern PGconn* DB;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Envoie une réponse JSON formatée.
 *
 * @param c Connexion Mongoose
 * @param j Objet JSON à envoyer
 * @param code Code HTTP de réponse
 */
static void send_json_reply(struct mg_connection* c, cJSON* j, int code) {
	char *text = cJSON_PrintUnformatted(j);
	mg_http_reply(c, code, "Content-Type: application/json\r\n", "%s", text);
	free(text);
	if (j) cJSON_Delete(j);
}

/**
 * Vérifie si l utilisateur courant est administrateur.
 *
 * @param hm Message HTTP contenant le cookie
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* hm) {
	int user_id = get_current_user_id(hm);
	if (user_id == 0) return 0;
	return user_repo_is_admin(DB, user_id);
}

/**
 * Génère un sel aléatoire pour le hachage.
 *
 * @param salt Buffer pour stocker le sel
 * @param len Longueur du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t len) {
	for (size_t idx = 0; idx < len; idx++) salt[idx] = (uint8_t)rand();
}

/**
 * Parse le JSON du body et valide les champs requis pour création user.
 *
 * @param hm Message HTTP
 * @param name_out Pointeur vers le nom extrait
 * @param email_out Pointeur vers l email extrait
 * @param pwd_out Pointeur vers le password extrait
 * @return Objet cJSON parsé ou NULL si erreur
 */
static cJSON* parse_user_create_json(struct mg_http_message *hm, const char **name_out, const char **email_out, const char **pwd_out) {
	cJSON* json = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!json) return NULL;
	*name_out = cJSON_GetStringValue(cJSON_GetObjectItem(json, "name"));
	*email_out = cJSON_GetStringValue(cJSON_GetObjectItem(json, "email"));
	*pwd_out = cJSON_GetStringValue(cJSON_GetObjectItem(json, "password"));
	if (!*name_out || !*email_out || !*pwd_out) { cJSON_Delete(json); return NULL; }
	return json;
}

/**
 * Hash le mot de passe avec Argon2id.
 *
 * @param pwd Mot de passe en clair
 * @param encoded_hash Buffer pour le hash encodé
 * @param hash_size Taille du buffer
 * @return 1 si succès, 0 si erreur
 */
static int hash_password(const char* pwd, char* encoded_hash, size_t hash_size) {
	uint8_t salt[16];
	generate_salt(salt, sizeof(salt));
	return argon2id_hash_encoded(2, 1 << 16, 1, pwd, strlen(pwd), salt, sizeof(salt), 32, encoded_hash, hash_size) == ARGON2_OK;
}

/**
 * Remplit la structure User avec les données parsées.
 *
 * @param u Structure User à remplir
 * @param name_str Nom
 * @param email_str Email
 * @param encoded_hash Hash du mot de passe
 * @param j JSON pour extraire le champ admin
 */
static void fill_user_struct(User* u, const char* name_str, const char* email_str, const char* encoded_hash, cJSON* j) {
	snprintf(u->name, sizeof(u->name), "%s", name_str);
	snprintf(u->email, sizeof(u->email), "%s", email_str);
	snprintf(u->password_hash, sizeof(u->password_hash), "%s", encoded_hash);
	u->is_admin = cJSON_IsTrue(cJSON_GetObjectItem(j, "admin"));
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Crée un nouvel utilisateur (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les données
 */
void user_create(struct mg_connection* c, struct mg_http_message *hm) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	const char *name_str, *email_str, *pwd_str;
	cJSON* j = parse_user_create_json(hm, &name_str, &email_str, &pwd_str);
	if (!j) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON or missing fields\"}"); return; }
	char encoded_hash[128];
	if (!hash_password(pwd_str, encoded_hash, sizeof(encoded_hash))) { cJSON_Delete(j); mg_http_reply(c, 500, "Content-Type: application/json\r\n", "{\"error\":\"Hashing failed\"}"); return; }
	User u = {0};
	fill_user_struct(&u, name_str, email_str, encoded_hash, j);
	u.id = user_repo_add(DB, &u);
	cJSON_Delete(j);
	cJSON *response = cJSON_CreateObject();
	cJSON_AddNumberToObject(response, "id", u.id);
	send_json_reply(c, response, 201);
}

/**
 * Construit l objet JSON pour un utilisateur.
 *
 * @param u Pointeur vers la structure User
 * @return Objet cJSON créé
 */
static cJSON* build_user_json(User* u) {
	cJSON* json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", u->id);
	cJSON_AddStringToObject(json_obj, "name", u->name);
	cJSON_AddStringToObject(json_obj, "email", u->email);
	cJSON_AddBoolToObject(json_obj, "admin", u->is_admin);
	return json_obj;
}

/**
 * Récupère un utilisateur par son ID.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 * @param id ID de l utilisateur
 */
void user_get(struct mg_connection* c, struct mg_http_message *hm, int id) {
	User u;
	if (!user_repo_find(DB, id, &u)) { mg_http_reply(c, 404, "", ""); return; }
	send_json_reply(c, build_user_json(&u), 200);
	(void)hm;
}

/**
 * Vérifie les droits pour modifier un utilisateur.
 *
 * @param hm Message HTTP
 * @param id ID de l utilisateur cible
 * @param current_user_is_admin Pointeur pour stocker si admin
 * @return 1 si autorisé, 0 sinon
 */
static int check_patch_rights(struct mg_http_message *hm, int id, int* current_user_is_admin) {
	int current_user_id = get_current_user_id(hm);
	*current_user_is_admin = user_repo_is_admin(DB, current_user_id);
	return (current_user_id == id || *current_user_is_admin);
}

/**
 * Modifie un utilisateur existant (accès admin ou self).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP contenant les données
 * @param id ID de l utilisateur
 */
void user_patch(struct mg_connection* c, struct mg_http_message *hm, int id) {
	int current_user_is_admin;
	if (!check_patch_rights(hm, id, &current_user_is_admin)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON* j = cJSON_ParseWithLength(hm->body.buf, hm->body.len);
	if (!j) { mg_http_reply(c, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	if (!current_user_is_admin && cJSON_HasObjectItem(j, "admin")) cJSON_DeleteItemFromObject(j, "admin");
	user_repo_patch(DB, id, j);
	cJSON_Delete(j);
	mg_http_reply(c, 200, "", "");
}

/**
 * Supprime un utilisateur (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 * @param id ID de l utilisateur
 */
void user_del(struct mg_connection* c, struct mg_http_message *hm, int id) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	user_repo_del(DB, id);
	mg_http_reply(c, 200, "", "");
}

/**
 * Callback pour ajouter un utilisateur à un tableau JSON.
 *
 * @param u Pointeur vers l utilisateur
 * @param arg Tableau JSON cible
 */
static void admin_users_list_cb(User* u, void* arg) {
	cJSON* arr = (cJSON*)arg;
	cJSON_AddItemToArray(arr, build_user_json(u));
}

/**
 * Liste tous les utilisateurs (accès admin).
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP reçu
 */
void admin_users_list(struct mg_connection* c, struct mg_http_message *hm) {
	if (!is_admin(hm)) { mg_http_reply(c, 403, "", ""); return; }
	cJSON* arr = cJSON_CreateArray();
	user_repo_each(DB, admin_users_list_cb, arr);
	send_json_reply(c, arr, 200);
}
