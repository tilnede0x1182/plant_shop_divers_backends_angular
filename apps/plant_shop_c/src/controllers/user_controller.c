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
extern PGconn* DATABASE_CONNECTION;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Envoie une réponse JSON formatée.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param json_object Objet JSON à envoyer
 * @param code Code HTTP de réponse
 */
static void send_json_reply(struct mg_connection* mongoose_connection, cJSON* json_object, int code) {
	char *text = cJSON_PrintUnformatted(json_object);
	mg_http_reply(mongoose_connection, code, "Content-Type: application/json\r\n", "%s", text);
	free(text);
	if (json_object) cJSON_Delete(json_object);
}

/**
 * Vérifie si l utilisateur courant est administrateur.
 *
 * @param http_message Message HTTP contenant le cookie
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* http_message) {
	int user_id = get_current_user_id(http_message);
	if (user_id == 0) return 0;
	return user_repo_is_admin(DATABASE_CONNECTION, user_id);
}

/**
 * Génère un sel aléatoire pour le hachage.
 *
 * @param salt Buffer pour stocker le sel
 * @param salt_length Longueur du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t salt_length) {
	for (size_t idx = 0; idx < salt_length; idx++) salt[idx] = (uint8_t)rand();
}

/**
 * Parse le JSON du body et valide les champs requis pour création user.
 *
 * @param http_message Message HTTP
 * @param name_out Pointeur vers le nom extrait
 * @param email_out Pointeur vers l email extrait
 * @param password_output Pointeur vers le password extrait
 * @return Objet cJSON parsé ou NULL si erreur
 */
static cJSON* parse_user_create_json(struct mg_http_message *http_message, const char **name_out, const char **email_out, const char **password_output) {
	cJSON* json = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json) return NULL;
	*name_out = cJSON_GetStringValue(cJSON_GetObjectItem(json, "name"));
	*email_out = cJSON_GetStringValue(cJSON_GetObjectItem(json, "email"));
	*password_output = cJSON_GetStringValue(cJSON_GetObjectItem(json, "password"));
	if (!*name_out || !*email_out || !*password_output) { cJSON_Delete(json); return NULL; }
	return json;
}

/**
 * Hash le mot de passe avec Argon2id.
 *
 * @param password_clear Mot de passe en clair
 * @param encoded_hash Buffer pour le hash encodé
 * @param hash_size Taille du buffer
 * @return 1 si succès, 0 si erreur
 */
static int hash_password(const char* password_clear, char* encoded_hash, size_t hash_size) {
	uint8_t salt[16];
	generate_salt(salt, sizeof(salt));
	return argon2id_hash_encoded(2, 1 << 16, 1, password_clear, strlen(password_clear), salt, sizeof(salt), 32, encoded_hash, hash_size) == ARGON2_OK;
}

/**
 * Remplit la structure User avec les données parsées.
 *
 * @param user_struct Structure User à remplir
 * @param name_str Nom
 * @param email_str Email
 * @param encoded_hash Hash du mot de passe
 * @param json_data JSON pour extraire le champ admin
 */
static void fill_user_struct(User* user_struct, const char* name_str, const char* email_str, const char* encoded_hash, cJSON* json_data) {
	snprintf(user_struct->name, sizeof(user_struct->name), "%s", name_str);
	snprintf(user_struct->email, sizeof(user_struct->email), "%s", email_str);
	snprintf(user_struct->password_hash, sizeof(user_struct->password_hash), "%s", encoded_hash);
	user_struct->is_admin = cJSON_IsTrue(cJSON_GetObjectItem(json_data, "admin"));
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Crée un nouvel utilisateur (accès admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les données
 */
void user_create(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	const char *name_str, *email_str, *pwd_str;
	cJSON* json_obj = parse_user_create_json(http_message, &name_str, &email_str, &pwd_str);
	if (!json_obj) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON or missing fields\"}"); return; }
	char encoded_hash[128];
	if (!hash_password(pwd_str, encoded_hash, sizeof(encoded_hash))) { cJSON_Delete(json_obj); mg_http_reply(mongoose_connection, 500, "Content-Type: application/json\r\n", "{\"error\":\"Hashing failed\"}"); return; }
	User user = {0};
	fill_user_struct(&user, name_str, email_str, encoded_hash, json_obj);
	user.id = user_repo_add(DATABASE_CONNECTION, &user);
	cJSON_Delete(json_obj);
	cJSON *response = cJSON_CreateObject();
	cJSON_AddNumberToObject(response, "id", user.id);
	send_json_reply(mongoose_connection, response, 201);
}

/**
 * Construit l objet JSON pour un utilisateur.
 *
 * @param user Pointeur vers la structure User
 * @return Objet cJSON créé
 */
static cJSON* build_user_json(User* user) {
	cJSON* json_obj = cJSON_CreateObject();
	cJSON_AddNumberToObject(json_obj, "id", user->id);
	cJSON_AddStringToObject(json_obj, "name", user->name);
	cJSON_AddStringToObject(json_obj, "email", user->email);
	cJSON_AddBoolToObject(json_obj, "admin", user->is_admin);
	return json_obj;
}

/**
 * Récupère un utilisateur par son ID.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP reçu
 * @param user_id ID de l utilisateur
 */
void user_get(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int user_id) {
	User user;
	if (!user_repo_find(DATABASE_CONNECTION, user_id, &user)) { mg_http_reply(mongoose_connection, 404, "", ""); return; }
	send_json_reply(mongoose_connection, build_user_json(&user), 200);
	(void)http_message;
}

/**
 * Vérifie les droits pour modifier un utilisateur.
 *
 * @param http_message Message HTTP
 * @param target_user_id ID de l utilisateur cible
 * @param current_user_is_admin Pointeur pour stocker si admin
 * @return 1 si autorisé, 0 sinon
 */
static int check_patch_rights(struct mg_http_message *http_message, int target_user_id, int* current_user_is_admin) {
	int current_user_id = get_current_user_id(http_message);
	*current_user_is_admin = user_repo_is_admin(DATABASE_CONNECTION, current_user_id);
	return (current_user_id == target_user_id || *current_user_is_admin);
}

/**
 * Modifie un utilisateur existant (accès admin ou self).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les données
 * @param id ID de l utilisateur
 */
void user_patch(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int user_id) {
	int current_user_is_admin;
	if (!check_patch_rights(http_message, user_id, &current_user_is_admin)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON* json_obj = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json_obj) { mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}"); return; }
	if (!current_user_is_admin && cJSON_HasObjectItem(json_obj, "admin")) cJSON_DeleteItemFromObject(json_obj, "admin");
	user_repo_patch(DATABASE_CONNECTION, user_id, json_obj);
	cJSON_Delete(json_obj);
	mg_http_reply(mongoose_connection, 200, "", "");
}

/**
 * Supprime un utilisateur (accès admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP reçu
 * @param user_id ID de l utilisateur
 */
void user_del(struct mg_connection* mongoose_connection, struct mg_http_message *http_message, int user_id) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	user_repo_del(DATABASE_CONNECTION, user_id);
	mg_http_reply(mongoose_connection, 200, "", "");
}

/**
 * Callback pour ajouter un utilisateur à un tableau JSON.
 *
 * @param user_data Pointeur vers l utilisateur
 * @param callback_data Tableau JSON cible
 */
static void admin_users_list_cb(User* user_data, void* callback_data) {
	cJSON* json_array = (cJSON*)callback_data;
	cJSON_AddItemToArray(json_array, build_user_json(user_data));
}

/**
 * Liste tous les utilisateurs (accès admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP reçu
 */
void admin_users_list(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	if (!is_admin(http_message)) { mg_http_reply(mongoose_connection, 403, "", ""); return; }
	cJSON* json_array = cJSON_CreateArray();
	user_repo_each(DATABASE_CONNECTION, admin_users_list_cb, json_array);
	send_json_reply(mongoose_connection, json_array, 200);
}
