/* ==============================================================================
   Importations
   ============================================================================== */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <curl/curl.h>
#include <cjson/cJSON.h>

#define BASE_URL "http://localhost:4100/api"
#define ADMIN_EMAIL "admin1@planteshop.com"
#define ADMIN_PASSWORD "password"

typedef struct { CURL *curl; char cookie_jar[L_tmpnam]; } TestSession;
struct MemoryStruct { char *memory; size_t size; };

static TestSession admin_session;
static TestSession user_session;
static char timestamp_str[20];

/* ---------- Utilitaires HTTP ---------- */

/**
 * Callback cURL pour stocker la réponse HTTP en mémoire.
 *
 * @param contents Données reçues
 * @param size Taille d un élément
 * @param nmemb Nombre d éléments
 * @param userp Pointeur vers le MemoryStruct
 * @return Nombre d octets traités
 */
static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
	size_t realsize = size * nmemb;
	struct MemoryStruct *mem = (struct MemoryStruct *)userp;
	char *ptr = realloc(mem->memory, mem->size + realsize + 1);
	if (!ptr) { printf("Pas assez de mémoire\n"); return 0; }
	mem->memory = ptr;
	memcpy(&(mem->memory[mem->size]), contents, realsize);
	mem->size += realsize;
	mem->memory[mem->size] = 0;
	return realsize;
}

/**
 * Tente une connexion socket au serveur.
 *
 * @param host Adresse IP
 * @param port Port
 * @return 1 si connecté, 0 sinon
 */
static int try_connect(const char* host, unsigned short port) {
	int sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd < 0) return 0;
	struct sockaddr_in addr = {0};
	addr.sin_family = AF_INET;
	addr.sin_port = htons(port);
	inet_pton(AF_INET, host, &addr.sin_addr);
	int result = connect(sockfd, (struct sockaddr*)&addr, sizeof(addr));
	close(sockfd);
	return result == 0;
}

/**
 * Attend que le serveur soit accessible.
 *
 * @param host Adresse IP du serveur
 * @param port Port du serveur
 * @param timeout_ms Délai maximum en millisecondes
 * @return 1 si serveur accessible, 0 si timeout
 */
int waitForServer(const char* host, unsigned short port, int timeout_ms) {
	struct timespec start, now;
	clock_gettime(CLOCK_MONOTONIC, &start);
	do {
		if (try_connect(host, port)) return 1;
		usleep(100 * 1000);
		clock_gettime(CLOCK_MONOTONIC, &now);
	} while (((now.tv_sec - start.tv_sec) * 1000 + (now.tv_nsec - start.tv_nsec) / 1000000) < timeout_ms);
	return 0;
}

/**
 * Initialise une session de test.
 *
 * @param session Pointeur vers la TestSession
 */
void init_session(TestSession *session) {
	session->curl = curl_easy_init();
	if (!session->curl) { fprintf(stderr, "curl_easy_init() a échoué\n"); exit(1); }
	tmpnam(session->cookie_jar);
	curl_easy_setopt(session->curl, CURLOPT_COOKIEJAR, session->cookie_jar);
	curl_easy_setopt(session->curl, CURLOPT_COOKIEFILE, session->cookie_jar);
}

/**
 * Libère les ressources d une session de test.
 *
 * @param session Pointeur vers la TestSession
 */
void cleanup_session(TestSession *session) {
	curl_easy_cleanup(session->curl);
	remove(session->cookie_jar);
}

/* ---------- API Call ---------- */

/**
 * Effectue un appel API générique.
 *
 * @param who "admin" ou "user"
 * @param method Méthode HTTP
 * @param path Chemin API
 * @param expected_status Code HTTP attendu
 * @param body_json Corps JSON (peut être NULL)
 * @return Réponse JSON parsée ou NULL
 */
cJSON* api_call(const char* who, const char* method, const char* path, int expected_status, const cJSON* body_json);

/**
 * Prépare les options cURL pour un appel API.
 *
 * @param curl Handle cURL
 * @param url URL complète
 * @param method Méthode HTTP
 * @param chunk Structure pour la réponse
 */
static void setup_curl_opts(CURL *curl, const char* url, const char* method, struct MemoryStruct *chunk) {
	curl_easy_setopt(curl, CURLOPT_URL, url);
	curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
	curl_easy_setopt(curl, CURLOPT_WRITEDATA, chunk);
	curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, method);
}

/**
 * Vérifie le résultat d un appel API.
 *
 * @param res Code retour cURL
 * @param response_code Code HTTP reçu
 * @param expected_status Code HTTP attendu
 * @param method Méthode HTTP
 * @param path Chemin API
 * @param response Réponse textuelle
 */
static void check_api_result(CURLcode res, long response_code, int expected_status, const char* method, const char* path, const char* response) {
	printf("%s %-7s%s [%ld]\n", (response_code == expected_status) ? "✅" : "❌", method, path, response_code);
	if (res != CURLE_OK) { fprintf(stderr, "curl error: %s\n", curl_easy_strerror(res)); exit(1); }
	if (response_code != expected_status) { fprintf(stderr, "API error: got %ld, want %d\n", response_code, expected_status); exit(1); }
}

cJSON* api_call(const char* who, const char* method, const char* path, int expected_status, const cJSON* body_json) {
	TestSession *session = (strcmp(who, "admin") == 0) ? &admin_session : &user_session;
	char url[256]; snprintf(url, sizeof(url), "%s%s", BASE_URL, path);
	struct MemoryStruct chunk = {0};
	setup_curl_opts(session->curl, url, method, &chunk);
	struct curl_slist *headers = NULL;
	char *json_str = NULL;
	if (body_json) { json_str = cJSON_PrintUnformatted(body_json); headers = curl_slist_append(headers, "Content-Type: application/json"); curl_easy_setopt(session->curl, CURLOPT_POSTFIELDS, json_str); }
	else if (strcmp(method, "POST") == 0 || strcmp(method, "PATCH") == 0) { curl_easy_setopt(session->curl, CURLOPT_POSTFIELDS, ""); }
	curl_easy_setopt(session->curl, CURLOPT_HTTPHEADER, headers);
	CURLcode res = curl_easy_perform(session->curl);
	long response_code; curl_easy_getinfo(session->curl, CURLINFO_RESPONSE_CODE, &response_code);
	check_api_result(res, response_code, expected_status, method, path, chunk.memory);
	cJSON *json_response = (chunk.memory && strlen(chunk.memory) > 0) ? cJSON_Parse(chunk.memory) : NULL;
	free(chunk.memory); if (json_str) free(json_str); if (headers) curl_slist_free_all(headers);
	return json_response;
}

/* ---------- Assertions ---------- */

/**
 * Vérifie qu une clé JSON est un nombre.
 *
 * @param obj Objet JSON parent
 * @param key Nom de la clé
 */
void assert_is_number(cJSON* obj, const char* key) {
	if (!cJSON_HasObjectItem(obj, key) || !cJSON_IsNumber(cJSON_GetObjectItem(obj, key))) { fprintf(stderr, "'%s' n'est pas un nombre\n", key); exit(1); }
}

/**
 * Vérifie qu une clé JSON contient une chaîne attendue.
 *
 * @param obj Objet JSON parent
 * @param key Nom de la clé
 * @param expected Valeur attendue
 */
void assert_eq_str(cJSON* obj, const char* key, const char* expected) {
	cJSON* item = cJSON_GetObjectItem(obj, key);
	int ok = item && cJSON_IsString(item) && strcmp(item->valuestring, expected) == 0;
	printf("%s   ↳ %s=\"%s\"\n", ok ? "✅" : "❌", key, item ? item->valuestring : "NULL");
	if (!ok) exit(1);
}

/**
 * Vérifie qu une clé JSON contient un entier attendu.
 *
 * @param obj Objet JSON parent
 * @param key Nom de la clé
 * @param expected Valeur attendue
 */
void assert_eq_int(cJSON* obj, const char* key, int expected) {
	cJSON* item = cJSON_GetObjectItem(obj, key);
	int ok = item && cJSON_IsNumber(item) && item->valueint == expected;
	printf("%s   ↳ %s=%d\n", ok ? "✅" : "❌", key, item ? item->valueint : -1);
	if (!ok) exit(1);
}

/**
 * Vérifie qu une clé JSON contient un booléen attendu.
 *
 * @param obj Objet JSON parent
 * @param key Nom de la clé
 * @param expected 1 pour true, 0 pour false
 */
void assert_eq_bool(cJSON* obj, const char* key, int expected) {
	cJSON* item = cJSON_GetObjectItem(obj, key);
	int ok = item && cJSON_IsBool(item) && cJSON_IsTrue(item) == expected;
	printf("%s   ↳ %s=%s\n", ok ? "✅" : "❌", key, cJSON_IsTrue(item) ? "true" : "false");
	if (!ok) exit(1);
}

/* ---------- Tests Plantes ---------- */

/**
 * Crée une plante de test et retourne son ID.
 *
 * @return ID de la plante créée
 */
static int create_test_plant(void) {
	cJSON *data = cJSON_CreateObject();
	cJSON_AddStringToObject(data, "name", "Test Plant C");
	cJSON_AddNumberToObject(data, "price", 10);
	cJSON_AddNumberToObject(data, "stock", 5);
	cJSON *plant = api_call("admin", "POST", "/admin/plants", 201, data);
	int id = cJSON_GetObjectItem(plant, "id")->valueint;
	cJSON_Delete(data); cJSON_Delete(plant);
	return id;
}

/**
 * Vérifie les opérations GET/PATCH sur une plante.
 *
 * @param id ID de la plante
 */
static void verify_plant_operations(int id) {
	char path[64]; snprintf(path, sizeof(path), "/plants/%d", id);
	cJSON *get = api_call("admin", "GET", path, 200, NULL);
	assert_eq_str(get, "name", "Test Plant C"); cJSON_Delete(get);
	cJSON *upd = cJSON_CreateObject(); cJSON_AddNumberToObject(upd, "price", 15);
	snprintf(path, sizeof(path), "/admin/plants/%d", id);
	api_call("admin", "PATCH", path, 200, upd); cJSON_Delete(upd);
	snprintf(path, sizeof(path), "/plants/%d", id);
	cJSON *check = api_call("admin", "GET", path, 200, NULL);
	assert_eq_int(check, "price", 15); cJSON_Delete(check);
}

/**
 * Teste les opérations CRUD sur les plantes.
 */
void test_plants(void) {
	printf("\n📌 TEST MODULE: PLANTS\n");
	int id = create_test_plant();
	verify_plant_operations(id);
	char path[64]; snprintf(path, sizeof(path), "/admin/plants/%d", id);
	api_call("admin", "DELETE", path, 200, NULL);
}

/* ---------- Tests Utilisateurs ---------- */

/**
 * Crée un utilisateur de test et retourne son ID.
 *
 * @return ID de l utilisateur créé
 */
static int create_test_user(void) {
	char email[64]; snprintf(email, sizeof(email), "utilisateur_test_c_%s@example.com", timestamp_str);
	cJSON *data = cJSON_CreateObject();
	cJSON_AddStringToObject(data, "email", email);
	cJSON_AddStringToObject(data, "name", "Utilisateur de test C");
	cJSON_AddStringToObject(data, "password", "pass123");
	cJSON *user = api_call("admin", "POST", "/users", 201, data);
	int id = cJSON_GetObjectItem(user, "id")->valueint;
	cJSON_Delete(data); cJSON_Delete(user);
	return id;
}

/**
 * Teste les opérations CRUD sur les utilisateurs.
 */
void test_users(void) {
	printf("\n📌 TEST MODULE: USERS\n");
	int id = create_test_user();
	char path[64]; snprintf(path, sizeof(path), "/users/%d", id);
	cJSON *upd = cJSON_CreateObject(); cJSON_AddStringToObject(upd, "name", "Tester Update C");
	api_call("admin", "PATCH", path, 200, upd); cJSON_Delete(upd);
	cJSON *get = api_call("admin", "GET", path, 200, NULL);
	assert_eq_str(get, "name", "Tester Update C"); cJSON_Delete(get);
	api_call("admin", "DELETE", path, 200, NULL);
}

/* ---------- Tests Commandes ---------- */

/**
 * Crée une plante pour tester les commandes.
 *
 * @return ID de la plante créée
 */
static int create_order_test_plant(void) {
	char name[64]; snprintf(name, sizeof(name), "Plante_de_test_c_%s", timestamp_str);
	cJSON *data = cJSON_CreateObject();
	cJSON_AddStringToObject(data, "name", name);
	cJSON_AddNumberToObject(data, "price", 10);
	cJSON_AddNumberToObject(data, "stock", 5);
	cJSON *plant = api_call("admin", "POST", "/admin/plants", 201, data);
	int id = cJSON_GetObjectItem(plant, "id")->valueint;
	cJSON_Delete(data); cJSON_Delete(plant);
	return id;
}

/**
 * Crée une commande pour un produit.
 *
 * @param pid ID de la plante
 * @return ID de la commande créée
 */
static int create_order_for_plant(int pid) {
	cJSON *data = cJSON_CreateObject();
	cJSON *items = cJSON_AddArrayToObject(data, "items");
	cJSON *item = cJSON_CreateObject();
	cJSON_AddNumberToObject(item, "plantId", pid);
	cJSON_AddNumberToObject(item, "quantity", 2);
	cJSON_AddItemToArray(items, item);
	cJSON *order = api_call("user", "POST", "/orders", 201, data);
	int id = cJSON_GetObjectItem(order, "id")->valueint;
	cJSON_Delete(data); cJSON_Delete(order);
	return id;
}

/**
 * Vérifie le statut d une commande après mise à jour.
 *
 * @param oid ID de la commande
 */
static void verify_order_status(int oid) {
	cJSON *upd = cJSON_CreateObject(); cJSON_AddStringToObject(upd, "status", "shipped");
	char path[64]; snprintf(path, sizeof(path), "/orders/%d", oid);
	api_call("admin", "PATCH", path, 200, upd); cJSON_Delete(upd);
	cJSON *list = api_call("user", "GET", "/orders", 200, NULL);
	cJSON *found = NULL, *o;
	cJSON_ArrayForEach(o, list) { if (cJSON_GetObjectItem(o, "id")->valueint == oid) { found = o; break; } }
	if (!found) { fprintf(stderr, "Commande non trouvée\n"); exit(1); }
	assert_eq_str(found, "status", "shipped"); cJSON_Delete(list);
}

/**
 * Teste les opérations sur les commandes.
 */
void test_orders(void) {
	printf("\n📌 TEST MODULE: ORDERS\n");
	int pid = create_order_test_plant();
	int oid = create_order_for_plant(pid);
	verify_order_status(oid);
	char path[64]; snprintf(path, sizeof(path), "/orders/%d", oid); api_call("admin", "DELETE", path, 200, NULL);
	snprintf(path, sizeof(path), "/admin/plants/%d", pid); api_call("admin", "DELETE", path, 200, NULL);
}

/* ---------- Tests Profil ---------- */

/**
 * Trouve l ID d un utilisateur par email.
 *
 * @param email Email recherché
 * @return ID de l utilisateur
 */
static int find_user_id_by_email(const char* email) {
	cJSON *users = api_call("admin", "GET", "/users", 200, NULL);
	cJSON *u = NULL, *cur;
	cJSON_ArrayForEach(cur, users) {
		cJSON *e = cJSON_GetObjectItem(cur, "email");
		if (e && cJSON_IsString(e) && strcmp(e->valuestring, email) == 0) { u = cur; break; }
	}
	if (!u) { fprintf(stderr, "Utilisateur introuvable\n"); exit(1); }
	int uid = cJSON_GetObjectItem(u, "id")->valueint;
	cJSON_Delete(users);
	return uid;
}

/**
 * Teste la consultation et modification du profil utilisateur.
 *
 * @param user_email Email de l utilisateur
 */
void test_user_profile(const char* user_email) {
	printf("\n📌 TEST MODULE: USER PROFILE\n");
	int uid = find_user_id_by_email(user_email);
	char path[64]; snprintf(path, sizeof(path), "/users/%d", uid);
	cJSON *profile = api_call("user", "GET", path, 200, NULL);
	assert_eq_int(profile, "id", uid); cJSON_Delete(profile);
	char new_name[64]; snprintf(new_name, sizeof(new_name), "Utilisateur_de_test_c_%s", timestamp_str);
	cJSON *upd = cJSON_CreateObject(); cJSON_AddStringToObject(upd, "name", new_name);
	api_call("user", "PATCH", path, 200, upd); cJSON_Delete(upd);
	cJSON *updated = api_call("user", "GET", path, 200, NULL);
	assert_eq_str(updated, "name", new_name); cJSON_Delete(updated);
}

/**
 * Teste les opérations admin sur les plantes.
 */
void test_admin_plants(void) {
	printf("\n📌 TEST MODULE: ADMIN PLANTS\n");
	cJSON *list = api_call("admin", "GET", "/admin/plants", 200, NULL);
	printf("   ↳ %d plantes\n", cJSON_GetArraySize(list)); cJSON_Delete(list);
	char name[64]; snprintf(name, sizeof(name), "Plante_admin_c_%s", timestamp_str);
	cJSON *data = cJSON_CreateObject(); cJSON_AddStringToObject(data, "name", name);
	cJSON_AddNumberToObject(data, "price", 99); cJSON_AddNumberToObject(data, "stock", 12);
	cJSON *p = api_call("admin", "POST", "/admin/plants", 201, data);
	int pid = cJSON_GetObjectItem(p, "id")->valueint; cJSON_Delete(data); cJSON_Delete(p);
	char path[64]; snprintf(path, sizeof(path), "/admin/plants/%d", pid);
	cJSON *upd = cJSON_CreateObject(); cJSON_AddNumberToObject(upd, "price", 123);
	api_call("admin", "PATCH", path, 200, upd); cJSON_Delete(upd);
	api_call("admin", "DELETE", path, 200, NULL);
}

/* ---------- Tests Admin Users ---------- */

/**
 * Crée un admin temporaire pour les tests.
 *
 * @param email Buffer pour stocker l email créé
 * @param nom Buffer pour stocker le nom créé
 * @return ID de l admin créé
 */
static int create_temp_admin(char* email, char* nom) {
	snprintf(email, 64, "admin_temp_c_%s@example.com", timestamp_str);
	snprintf(nom, 64, "Admin Temporaire %s", timestamp_str);
	cJSON *data = cJSON_CreateObject();
	cJSON_AddStringToObject(data, "email", email); cJSON_AddStringToObject(data, "name", nom);
	cJSON_AddStringToObject(data, "password", "password"); cJSON_AddBoolToObject(data, "admin", 1);
	cJSON *tmp = api_call("admin", "POST", "/users", 201, data);
	int uid = cJSON_GetObjectItem(tmp, "id")->valueint;
	cJSON_Delete(data); cJSON_Delete(tmp);
	return uid;
}

/**
 * Vérifie qu un admin temporaire existe dans la liste.
 *
 * @param email Email à rechercher
 * @param nom Nom attendu
 */
static void verify_temp_admin_exists(const char* email, const char* nom) {
	cJSON *list = api_call("admin", "GET", "/admin/users", 200, NULL);
	cJSON *found = NULL, *u;
	cJSON_ArrayForEach(u, list) {
		cJSON *e = cJSON_GetObjectItem(u, "email");
		if (e && strcmp(e->valuestring, email) == 0) { found = u; break; }
	}
	if (!found) { fprintf(stderr, "Admin temporaire introuvable\n"); exit(1); }
	assert_eq_str(found, "name", nom); cJSON_Delete(list);
}

/**
 * Teste les opérations admin sur les utilisateurs.
 */
void test_admin_users(void) {
	printf("\n📌 TEST MODULE: ADMIN USERS\n");
	char email[64], nom[64];
	int uid = create_temp_admin(email, nom);
	verify_temp_admin_exists(email, nom);
	char new_nom[64]; snprintf(new_nom, sizeof(new_nom), "Admin_temp_modifié_%s", timestamp_str);
	cJSON *upd = cJSON_CreateObject(); cJSON_AddStringToObject(upd, "name", new_nom);
	char path[64]; snprintf(path, sizeof(path), "/users/%d", uid);
	api_call("admin", "PATCH", path, 200, upd); cJSON_Delete(upd);
	cJSON *user_get = api_call("admin", "GET", path, 200, NULL);
	assert_eq_str(user_get, "name", new_nom); cJSON_Delete(user_get);
	api_call("admin", "DELETE", path, 200, NULL);
}

/**
 * Teste les restrictions de rôles.
 */
void test_auth_roles(void) {
	printf("\n📌 TEST MODULE: ROLES\n");
	cJSON *bad = cJSON_CreateObject();
	cJSON_AddStringToObject(bad, "name", "Bad Plant");
	cJSON_AddNumberToObject(bad, "price", 1);
	cJSON_AddNumberToObject(bad, "stock", 1);
	api_call("user", "POST", "/admin/plants", 403, bad); cJSON_Delete(bad);
	api_call("user", "GET", "/users", 403, NULL);
}

/**
 * Teste le endpoint /auth/me.
 *
 * @param user_email Email attendu
 * @param user_name Nom attendu
 */
void test_auth_me(const char* user_email, const char* user_name) {
	printf("\n📌 TEST MODULE: AUTH /me\n");
	cJSON *me = api_call("user", "GET", "/auth/me", 200, NULL);
	assert_eq_str(me, "email", user_email);
	assert_eq_str(me, "name", user_name);
	cJSON_Delete(me);
}

/* ---------- Main ---------- */

/**
 * Génère le timestamp unique pour les tests.
 */
static void generate_timestamp(void) {
	time_t t = time(NULL);
	struct tm *tm = localtime(&t);
	strftime(timestamp_str, sizeof(timestamp_str), "%Y%m%d%H%M%S", tm);
}

/**
 * Initialise les sessions admin et user.
 *
 * @param user_email Buffer pour l email user créé
 */
static void setup_sessions(char* user_email) {
	cJSON *login = cJSON_CreateObject();
	cJSON_AddStringToObject(login, "email", ADMIN_EMAIL);
	cJSON_AddStringToObject(login, "password", ADMIN_PASSWORD);
	api_call("admin", "POST", "/auth/login", 201, login); cJSON_Delete(login);
	snprintf(user_email, 64, "user_c_%s@example.com", timestamp_str);
	cJSON *reg = cJSON_CreateObject();
	cJSON_AddStringToObject(reg, "name", "User C");
	cJSON_AddStringToObject(reg, "email", user_email);
	cJSON_AddStringToObject(reg, "password", "password123");
	api_call("user", "POST", "/auth/register", 201, reg);
	api_call("user", "POST", "/auth/login", 201, reg); cJSON_Delete(reg);
}

/**
 * Exécute tous les tests.
 *
 * @param user_email Email de l utilisateur de test
 */
static void run_all_tests(const char* user_email) {
	test_plants();
	test_users();
	test_orders();
	test_user_profile(user_email);
	test_auth_roles();
	test_admin_plants();
	test_admin_users();
	test_auth_me(user_email, "User C");
}

/**
 * Point d entrée des tests end-to-end.
 *
 * @return 0 si succès, 2 si serveur injoignable
 */
int main(void) {
	if (!waitForServer("127.0.0.1", 4100, 5000)) { fprintf(stderr, "❌ Serveur injoignable\n"); return 2; }
	curl_global_init(CURL_GLOBAL_ALL);
	init_session(&admin_session); init_session(&user_session);
	generate_timestamp();
	char user_email[64];
	printf("🧪 Démarrage des tests C: %s\n\n", BASE_URL);
	setup_sessions(user_email);
	run_all_tests(user_email);
	cleanup_session(&admin_session); cleanup_session(&user_session);
	curl_global_cleanup();
	printf("\n🎉 Tous les tests C ont réussi!\n");
	return 0;
}
