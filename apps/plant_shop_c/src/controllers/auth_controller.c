/* ==============================================================================
   Importations
   ============================================================================== */
#include "auth_controller.h"
#include <cjson/cJSON.h>
#include <argon2.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include "../repository/user_repository.h"
#include "../utils/utils.h"

/* ==============================================================================
   Données
   ============================================================================== */
extern PGconn* DATABASE_CONNECTION;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/* ------------------------------------------------------------------------------
   Réponses JSON
   ------------------------------------------------------------------------------ */
/**
 * Envoie une reponse JSON au client.
 *
 * @param connection Connexion Mongoose
 * @param json_obj Objet JSON a envoyer
 * @param code Code HTTP de reponse
 */
static void send_json(struct mg_connection* connection, cJSON* json_obj, int code) {
	char* json_str = cJSON_PrintUnformatted(json_obj);
	mg_http_reply(connection, code, "Content-Type: application/json\r\n", "%s", json_str);
	free(json_str);
	if (json_obj) cJSON_Delete(json_obj);
}

/**
 * Genere un sel aleatoire pour le hachage.
 *
 * @param salt Buffer pour stocker le sel
 * @param salt_length Longueur du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t len) {
	for (size_t idx = 0; idx < len; idx++) {
		salt[idx] = (uint8_t)rand();
	}
}

/**
 * Parse le JSON et extrait les champs requis.
 *
 * @param http_message Message HTTP recu
 * @param name Pointeur pour le champ name
 * @param email Pointeur pour le champ email
 * @param password Pointeur pour le champ password
 * @return Objet cJSON parse ou NULL si erreur
 */
static cJSON* parse_register_json(struct mg_http_message *http_message, const char **name,
								   const char **email, const char **password) {
	cJSON* json = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json) return NULL;
	*name = cJSON_GetStringValue(cJSON_GetObjectItem(json, "name"));
	*email = cJSON_GetStringValue(cJSON_GetObjectItem(json, "email"));
	*password = cJSON_GetStringValue(cJSON_GetObjectItem(json, "password"));
	return json;
}

/**
 * Hash un mot de passe avec Argon2id.
 *
 * @param password Mot de passe en clair
 * @param encoded_hash Buffer pour le hash encode
 * @param hash_size Taille du buffer
 * @return 1 si succes, 0 si erreur
 */
static int hash_password(const char* password, char* encoded_hash, size_t hash_size) {
	uint8_t salt[16];
	generate_salt(salt, sizeof(salt));
	int result = argon2id_hash_encoded(2, 1 << 16, 1, password, strlen(password),
										salt, sizeof(salt), 32, encoded_hash, hash_size);
	return result == ARGON2_OK;
}

/**
 * Cree un utilisateur a partir des donnees validees.
 *
 * @param name Nom de l utilisateur
 * @param email Email de l utilisateur
 * @param encoded_hash Hash du mot de passe
 * @return ID de l utilisateur cree ou 0 si erreur
 */
static int create_user_from_data(const char* name, const char* email, const char* encoded_hash) {
	User user = {.is_admin = 0};
	snprintf(user.name, sizeof(user.name), "%s", name);
	snprintf(user.email, sizeof(user.email), "%s", email);
	snprintf(user.password_hash, sizeof(user.password_hash), "%s", encoded_hash);
	user.id = user_repo_add(DATABASE_CONNECTION, &user);
	return user.id;
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Gere l inscription d un nouvel utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void auth_register(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	const char *name, *email, *password;
	cJSON* json = parse_register_json(http_message, &name, &email, &password);
	if (!json) { mg_http_reply(mongoose_connection, 400, "", "{\"error\":\"Invalid JSON\"}\n"); return; }
	if (!name || !email || !password) { cJSON_Delete(json); mg_http_reply(mongoose_connection, 400, "", "{\"error\":\"Missing fields\"}\n"); return; }
	char encoded_hash[128];
	if (!hash_password(password, encoded_hash, sizeof(encoded_hash))) { cJSON_Delete(json); mg_http_reply(mongoose_connection, 500, "", "{\"error\":\"Hashing failed\"}\n"); return; }
	int user_id = create_user_from_data(name, email, encoded_hash);
	cJSON_Delete(json);
	if (user_id == 0) { mg_http_reply(mongoose_connection, 409, "Content-Type: application/json\r\n", "{\"error\":\"Email already exists\"}\n"); return; }
	cJSON *response_json = cJSON_CreateObject();
	cJSON_AddNumberToObject(response_json, "id", user_id);
	send_json(mongoose_connection, response_json, 201);
}

/**
 * Parse le JSON de login et extrait email/password.
 *
 * @param http_message Message HTTP recu
 * @param email_buffer Buffer pour l email
 * @param password_buffer Buffer pour le password
 * @param buffer_size Taille des buffers
 * @return 1 si succes, 0 si erreur
 */
static int parse_login_json(struct mg_http_message *http_message, char* email_buffer,
							 char* password_buffer, size_t buffer_size) {
	cJSON* json = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
	if (!json) return 0;
	const char* email = cJSON_GetStringValue(cJSON_GetObjectItem(json, "email"));
	const char* password = cJSON_GetStringValue(cJSON_GetObjectItem(json, "password"));
	if (email) snprintf(email_buffer, buffer_size, "%s", email);
	if (password) snprintf(password_buffer, buffer_size, "%s", password);
	cJSON_Delete(json);
	return (email && password);
}

/**
 * Verifie les credentials et retourne l utilisateur.
 *
 * @param email_buffer Email de l utilisateur
 * @param password_buffer Mot de passe en clair
 * @param user Pointeur vers la structure User a remplir
 * @return 1 si credentials valides, 0 sinon
 */
static int verify_credentials(const char* email_buffer, const char* password_buffer, User* user) {
	if (!user_repo_find_by_mail(DATABASE_CONNECTION, email_buffer, user)) return 0;
	return argon2id_verify(user->password_hash, password_buffer, strlen(password_buffer)) == ARGON2_OK;
}

/**
 * Envoie la reponse de login avec le cookie de session.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param user Pointeur vers l utilisateur connecte
 */
static void send_login_response(struct mg_connection* mongoose_connection, User* user) {
	char cookie[256];
	snprintf(cookie, sizeof(cookie),
			 "Set-Cookie: plant_shop_c_backend=%d; Path=/; HttpOnly; Max-Age=86400", user->id);
	char headers[512];
	snprintf(headers, sizeof(headers),
			 "%s\r\nContent-Type: application/json\r\n", cookie);
	cJSON *response_json = cJSON_CreateObject();
	cJSON_AddStringToObject(response_json, "email", user->email);
	char* json_str = cJSON_PrintUnformatted(response_json);
	mg_http_reply(mongoose_connection, 201, headers, "%s", json_str);
	free(json_str);
	cJSON_Delete(response_json);
}

/**
 * Gere la connexion d un utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void auth_login(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	char email_buffer[128] = {0}, password_buffer[128] = {0};
	if (!parse_login_json(http_message, email_buffer, password_buffer, sizeof(email_buffer))) {
		mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n",
					  "{\"error\":\"Invalid JSON or missing fields\"}\n");
		return;
	}
	User user;
	if (!verify_credentials(email_buffer, password_buffer, &user)) {
		mg_http_reply(mongoose_connection, 401, "Content-Type: application/json\r\n",
					  "{\"error\":\"Invalid credentials\"}\n");
		return;
	}
	send_login_response(mongoose_connection, &user);
}

/**
 * Construit la reponse JSON pour auth_me.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param user Pointeur vers l utilisateur
 */
static void send_me_response(struct mg_connection* mongoose_connection, User* user) {
	cJSON* response_json = cJSON_CreateObject();
	cJSON_AddStringToObject(response_json, "email", user->email);
	cJSON_AddStringToObject(response_json, "name", user->name);
	cJSON_AddNumberToObject(response_json, "id", user->id);
	cJSON_AddBoolToObject(response_json, "admin", user->is_admin);
	send_json(mongoose_connection, response_json, 200);
}

/**
 * Retourne les informations de l utilisateur connecte.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void auth_me(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	char cookie_value_string[32] = {0};
	if (!get_cookie_manual(http_message, "plant_shop_c_backend", cookie_value_string, sizeof(cookie_value_string))) {
		mg_http_reply(mongoose_connection, 401, "Content-Type: application/json\r\n", "{\"error\":\"Unauthorized\"}\n");
		return;
	}
	int user_identifier = atoi(cookie_value_string);
	if (user_identifier == 0) { mg_http_reply(mongoose_connection, 401, "Content-Type: application/json\r\n", "{\"error\":\"Invalid token\"}\n"); return; }
	User user;
	if (!user_repo_find(DATABASE_CONNECTION, user_identifier, &user)) { mg_http_reply(mongoose_connection, 401, "Content-Type: application/json\r\n", "{\"error\":\"User not found\"}\n"); return; }
	send_me_response(mongoose_connection, &user);
}

/**
 * Deconnecte l utilisateur en supprimant le cookie.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void auth_logout(struct mg_connection* mongoose_connection, struct mg_http_message *http_message) {
	(void)http_message;
	mg_http_reply(mongoose_connection, 200, "Set-Cookie: plant_shop_c_backend=; Path=/; HttpOnly; Max-Age=0\r\n", "");
}
