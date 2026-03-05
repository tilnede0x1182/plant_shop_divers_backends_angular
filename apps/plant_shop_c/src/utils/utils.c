/* ==============================================================================
   Importations
   ============================================================================== */
#include "utils.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "mongoose/mongoose.h"

/**
 * Ouvre le fichier .env et quitte si erreur.
 *
 * @return Pointeur FILE vers .env
 */
static FILE* open_env_file(void) {
	FILE* fp = fopen(".env", "r");
	if (!fp) { perror("Impossible d'ouvrir .env"); exit(1); }
	return fp;
}

/**
 * Parse une ligne .env et extrait key/value.
 *
 * @param line Ligne à parser
 * @param key Buffer pour la clé
 * @param value_ptr Buffer pour la valeur
 * @return 1 si parsing OK, 0 sinon
 */
static int parse_env_line(char* line, char** key, char** value_ptr) {
	char* equals_sign = strchr(line, '=');
	if (!equals_sign) return 0;
	*equals_sign = '\0';
	*key = line;
	*value_ptr = equals_sign + 1;
	(*value_ptr)[strcspn(*value_ptr, "\r\n")] = '\0';
	return 1;
}

/**
 * Lit les variables de connexion DB depuis .env.
 *
 * @param url Buffer pour DATABASE_URL (min 128 chars)
 * @param user Buffer pour DATABASE_USER (min 64 chars)
 * @param pass Buffer pour DATABASE_PASS (min 64 chars)
 */
void read_db_env(char* url, char* user, char* pass) {
	FILE* fp = open_env_file();
	char line[256];
	while (fgets(line, sizeof(line), fp)) {
		char *key, *value_string;
		if (!parse_env_line(line, &key, &value_string)) continue;
		if (strcmp(key, "DATABASE_URL") == 0) { strncpy(url, value_string, 127); url[127] = '\0'; }
		else if (strcmp(key, "DATABASE_USER") == 0) { strncpy(user, value_string, 63); user[63] = '\0'; }
		else if (strcmp(key, "DATABASE_PASS") == 0) { strncpy(pass, value_string, 63); pass[63] = '\0'; }
	}
	fclose(fp);
}

/**
 * Lit les variables serveur depuis .env.
 *
 * @param port Buffer pour SERVER_ADDRESS (min 16 chars)
 * @param jwt_secret Buffer pour JWT_SECRET (min 128 chars)
 */
void read_server_env(char* port, char* jwt_secret) {
	FILE* fp = open_env_file();
	char line[256];
	while (fgets(line, sizeof(line), fp)) {
		char *key, *value_string;
		if (!parse_env_line(line, &key, &value_string)) continue;
		if (strcmp(key, "SERVER_ADDRESS") == 0) { strncpy(port, value_string, 15); port[15] = '\0'; }
		else if (strcmp(key, "JWT_SECRET") == 0) { strncpy(jwt_secret, value_string, 127); jwt_secret[127] = '\0'; }
	}
	fclose(fp);
}

/**
 * Copie le header Cookie dans un buffer.
 *
 * @param http_message Pointeur vers le message HTTP
 * @param buffer Buffer de destination
 * @param max_len Taille max du buffer
 * @return Longueur copiée, 0 si pas de header
 */
static size_t copy_cookie_header(struct mg_http_message* http_message, char* buffer, size_t max_len) {
	struct mg_str* header = mg_http_get_header(http_message, "Cookie");
	if (!header) return 0;
	size_t length = header->len < max_len ? header->len : max_len - 1;
	memcpy(buffer, header->buf, length);
	buffer[length] = '\0';
	return length;
}

/**
 * Cherche un cookie dans une chaîne tokenisée.
 *
 * @param cookie_buffer Buffer contenant les cookies
 * @param name Nom du cookie
 * @param output_buffer Buffer de sortie
 * @param buffer_size Taille du buffer
 * @return 1 si trouvé, 0 sinon
 */
static int find_cookie_token(char* cookie_buffer, const char* name, char* output_buffer, size_t buffer_size) {
	char* token = strtok(cookie_buffer, ";");
	size_t key_length = strlen(name);
	while (token) {
		while (*token == ' ') token++;
		if (strncmp(token, name, key_length) == 0 && token[key_length] == '=') {
			strncpy(output_buffer, token + key_length + 1, buffer_size - 1);
			output_buffer[buffer_size - 1] = '\0';
			return 1;
		}
		token = strtok(NULL, ";");
	}
	return 0;
}

/**
 * Extrait un cookie par son nom depuis l en-tête Cookie.
 *
 * @param http_message Message HTTP contenant les en-têtes
 * @param name Nom du cookie recherché
 * @param output_buffer Buffer de sortie pour la valeur
 * @param buffer_size Taille du buffer
 * @return 1 si trouvé, 0 sinon
 */
int get_cookie_manual(struct mg_http_message* http_message, const char* name, char* output_buffer, size_t buffer_size) {
	char cookie_buffer[1024];
	if (copy_cookie_header(http_message, cookie_buffer, sizeof(cookie_buffer)) == 0) return 0;
	return find_cookie_token(cookie_buffer, name, output_buffer, buffer_size);
}

/**
 * Récupère l ID utilisateur depuis le cookie de session.
 *
 * @param http_message Message HTTP contenant les cookies
 * @return ID utilisateur si connecté, 0 sinon
 */
int get_current_user_id(struct mg_http_message *http_message) {
	char jwt_value[32];
	if (!get_cookie_manual(http_message, "plant_shop_c_backend", jwt_value, sizeof(jwt_value))) return 0;
	int user_id = atoi(jwt_value);
	return user_id > 0 ? user_id : 0;
}
