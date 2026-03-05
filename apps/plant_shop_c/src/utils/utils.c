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
 * @param val Buffer pour la valeur
 * @return 1 si parsing OK, 0 sinon
 */
static int parse_env_line(char* line, char** key, char** val) {
	char* eq = strchr(line, '=');
	if (!eq) return 0;
	*eq = '\0';
	*key = line;
	*val = eq + 1;
	(*val)[strcspn(*val, "\r\n")] = '\0';
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
		char *key, *val;
		if (!parse_env_line(line, &key, &val)) continue;
		if (strcmp(key, "DATABASE_URL") == 0) { strncpy(url, val, 127); url[127] = '\0'; }
		else if (strcmp(key, "DATABASE_USER") == 0) { strncpy(user, val, 63); user[63] = '\0'; }
		else if (strcmp(key, "DATABASE_PASS") == 0) { strncpy(pass, val, 63); pass[63] = '\0'; }
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
		char *key, *val;
		if (!parse_env_line(line, &key, &val)) continue;
		if (strcmp(key, "SERVER_ADDRESS") == 0) { strncpy(port, val, 15); port[15] = '\0'; }
		else if (strcmp(key, "JWT_SECRET") == 0) { strncpy(jwt_secret, val, 127); jwt_secret[127] = '\0'; }
	}
	fclose(fp);
}

/**
 * Copie le header Cookie dans un buffer.
 *
 * @param http_message Message HTTP
 * @param buf Buffer de destination
 * @param max_len Taille max du buffer
 * @return Longueur copiée, 0 si pas de header
 */
static size_t copy_cookie_header(struct mg_http_message* hm, char* buf, size_t max_len) {
	struct mg_str* hdr = mg_http_get_header(hm, "Cookie");
	if (!hdr) return 0;
	size_t len = hdr->len < max_len ? hdr->len : max_len - 1;
	memcpy(buf, hdr->buf, len);
	buf[len] = '\0';
	return len;
}

/**
 * Cherche un cookie dans une chaîne tokenisée.
 *
 * @param buf Buffer contenant les cookies
 * @param name Nom du cookie
 * @param out Buffer de sortie
 * @param sz Taille du buffer
 * @return 1 si trouvé, 0 sinon
 */
static int find_cookie_token(char* buf, const char* name, char* out, size_t sz) {
	char* tok = strtok(buf, ";");
	size_t keylen = strlen(name);
	while (tok) {
		while (*tok == ' ') tok++;
		if (strncmp(tok, name, keylen) == 0 && tok[keylen] == '=') {
			strncpy(out, tok + keylen + 1, sz - 1);
			out[sz - 1] = '\0';
			return 1;
		}
		tok = strtok(NULL, ";");
	}
	return 0;
}

/**
 * Extrait un cookie par son nom depuis l en-tête Cookie.
 *
 * @param http_message Message HTTP contenant les en-têtes
 * @param name Nom du cookie recherché
 * @param out Buffer de sortie pour la valeur
 * @param sz Taille du buffer
 * @return 1 si trouvé, 0 sinon
 */
int get_cookie_manual(struct mg_http_message* hm, const char* name, char* out, size_t sz) {
	char buf[1024];
	if (copy_cookie_header(hm, buf, sizeof(buf)) == 0) return 0;
	return find_cookie_token(buf, name, out, sz);
}

/**
 * Récupère l ID utilisateur depuis le cookie de session.
 *
 * @param http_message Message HTTP contenant les cookies
 * @return ID utilisateur si connecté, 0 sinon
 */
int get_current_user_id(struct mg_http_message *hm) {
	char jwt_val[32];
	if (!get_cookie_manual(hm, "plant_shop_c_backend", jwt_val, sizeof(jwt_val))) return 0;
	int uid = atoi(jwt_val);
	return uid > 0 ? uid : 0;
}
