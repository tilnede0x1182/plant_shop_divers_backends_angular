#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <curl/curl.h>
#include <cjson/cJSON.h>

/*
 * ======================================================
 * 🧪 Tests End-to-End — Clone C de test_e2e.cpp
 * ======================================================
 */

#define BASE_URL "http://localhost:4100/api"
#define ADMIN_EMAIL "admin1@planteshop.com"
#define ADMIN_PASSWORD "password"

// Contexte pour une session (admin ou user)
typedef struct {
    CURL *curl;
    char cookie_jar[L_tmpnam];
} TestSession;

// Variables globales pour les sessions et le timestamp
static TestSession admin_session;
static TestSession user_session;
static char timestamp_str[20];

// ------------------------------------------------------
// ⚙️ Utilitaires et Contexte de Test
// ------------------------------------------------------

// Buffer pour les réponses HTTP de cURL
struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *)userp;
    char *ptr = realloc(mem->memory, mem->size + realsize + 1);
    if (!ptr) {
        printf("Pas assez de mémoire (realloc a échoué)\n");
        return 0;
    }
    mem->memory = ptr;
    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;
    return realsize;
}

// Vérifie si le serveur est prêt
int waitForServer(const char* host, unsigned short port, int timeout_ms) {
    struct timespec start, now;
    clock_gettime(CLOCK_MONOTONIC, &start);
    long elapsed_ms;

    do {
        int sockfd = socket(AF_INET, SOCK_STREAM, 0);
        if (sockfd < 0) return 0;

        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons(port);
        inet_pton(AF_INET, host, &addr.sin_addr);

        int result = connect(sockfd, (struct sockaddr*)&addr, sizeof(addr));
        close(sockfd);
        if (result == 0) return 1;

        usleep(100 * 1000); // 100ms
        clock_gettime(CLOCK_MONOTONIC, &now);
        elapsed_ms = (now.tv_sec - start.tv_sec) * 1000 + (now.tv_nsec - start.tv_nsec) / 1000000;
    } while (elapsed_ms < timeout_ms);

    return 0;
}


// Initialise une session de test (contexte cURL avec cookies)
void init_session(TestSession *session) {
    session->curl = curl_easy_init();
    if (!session->curl) {
        fprintf(stderr, "Erreur: curl_easy_init() a échoué\n");
        exit(1);
    }
    // Crée un nom de fichier temporaire pour le cookie jar
    tmpnam(session->cookie_jar);
    curl_easy_setopt(session->curl, CURLOPT_COOKIEJAR, session->cookie_jar);
    curl_easy_setopt(session->curl, CURLOPT_COOKIEFILE, session->cookie_jar);
}

void cleanup_session(TestSession *session) {
    curl_easy_cleanup(session->curl);
    remove(session->cookie_jar); // Supprime le fichier de cookies
}

// Requête API générique
cJSON* api_call(const char* who, const char* method, const char* path, int expected_status, const cJSON* body_json) {
    TestSession *session = (strcmp(who, "admin") == 0) ? &admin_session : &user_session;
    CURL *curl = session->curl;

    char url[256];
    snprintf(url, sizeof(url), "%s%s", BASE_URL, path);

    struct MemoryStruct chunk = {0};
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&chunk);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, method);

    struct curl_slist *headers = NULL;
    char *json_str = NULL;
    if (body_json) {
        json_str = cJSON_PrintUnformatted(body_json);
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json_str);
    } else {
        // Pour les requêtes PATCH/POST sans corps
        if (strcmp(method, "POST") == 0 || strcmp(method, "PATCH") == 0) {
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, "");
        }
    }
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    CURLcode res = curl_easy_perform(curl);
    long response_code;
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &response_code);

    printf("%s %-7s %-40s [%ld]\n", (response_code == expected_status) ? "✅" : "❌", method, path, response_code);

    if (res != CURLE_OK) {
        fprintf(stderr, "curl_easy_perform() a échoué: %s\n", curl_easy_strerror(res));
        exit(1);
    }
    if (response_code != expected_status) {
        fprintf(stderr, "Erreur API: Attendu %d, reçu %ld. Réponse: %s\n", expected_status, response_code, chunk.memory ? chunk.memory : "vide");
        exit(1);
    }

    cJSON *json_response = NULL;
    if (chunk.memory && strlen(chunk.memory) > 0) {
        json_response = cJSON_Parse(chunk.memory);
        if (!json_response) {
            fprintf(stderr, "Erreur de parsing JSON pour la réponse: %s\n", chunk.memory);
        }
    }

    free(chunk.memory);
    if (json_str) free(json_str);
    if (headers) curl_slist_free_all(headers);

    return json_response;
}

// ------------------------------------------------------
// 🔎 Fonctions d'Assertion
// ------------------------------------------------------

void assert_is_number(cJSON* obj, const char* key) {
    if (!cJSON_HasObjectItem(obj, key) || !cJSON_IsNumber(cJSON_GetObjectItem(obj, key))) {
        fprintf(stderr, "Assertion échouée: la clé '%s' n'est pas un nombre ou est absente.\n", key);
        exit(1);
    }
}

void assert_eq_str(cJSON* obj, const char* key, const char* expected) {
    cJSON* item = cJSON_GetObjectItem(obj, key);
    int ok = item && cJSON_IsString(item) && strcmp(item->valuestring, expected) == 0;
    printf("%s   ↳ %s=\"%s\" (attendu \"%s\")\n", ok ? "✅" : "❌", key, item ? item->valuestring : "NULL", expected);
    if (!ok) exit(1);
}

void assert_eq_int(cJSON* obj, const char* key, int expected) {
    cJSON* item = cJSON_GetObjectItem(obj, key);
    int ok = item && cJSON_IsNumber(item) && item->valueint == expected;
    printf("%s   ↳ %s=%d (attendu %d)\n", ok ? "✅" : "❌", key, item ? item->valueint : -1, expected);
    if (!ok) exit(1);
}

void assert_eq_bool(cJSON* obj, const char* key, int expected) {
    cJSON* item = cJSON_GetObjectItem(obj, key);
    int ok = item && cJSON_IsBool(item) && cJSON_IsTrue(item) == expected;
    printf("%s   ↳ %s=%s (attendu %s)\n", ok ? "✅" : "❌", key, cJSON_IsTrue(item) ? "true" : "false", expected ? "true" : "false");
    if (!ok) exit(1);
}

// ------------------------------------------------------
// 🧪 Modules de Test
// ------------------------------------------------------

void test_plants() {
    printf("\n📌 TEST MODULE: PLANTS (admin)\n");
    cJSON *plant_data = cJSON_CreateObject();
    cJSON_AddStringToObject(plant_data, "name", "Test Plant C");
    cJSON_AddNumberToObject(plant_data, "price", 10);
    cJSON_AddNumberToObject(plant_data, "stock", 5);

    cJSON *plant = api_call("admin", "POST", "/admin/plants", 201, plant_data);
    assert_is_number(plant, "id");
    int id = cJSON_GetObjectItem(plant, "id")->valueint;
    cJSON_Delete(plant_data);
    cJSON_Delete(plant);

    char path[64];
    snprintf(path, sizeof(path), "/plants/%d", id);
    cJSON *get = api_call("admin", "GET", path, 200, NULL);
    assert_eq_str(get, "name", "Test Plant C");
    cJSON_Delete(get);

    cJSON *price_update = cJSON_CreateObject();
    cJSON_AddNumberToObject(price_update, "price", 15);
    snprintf(path, sizeof(path), "/admin/plants/%d", id);
    api_call("admin", "PATCH", path, 200, price_update);
    cJSON_Delete(price_update);

    snprintf(path, sizeof(path), "/plants/%d", id);
    cJSON *check = api_call("admin", "GET", path, 200, NULL);
    assert_eq_int(check, "price", 15);
    cJSON_Delete(check);

    snprintf(path, sizeof(path), "/admin/plants/%d", id);
    api_call("admin", "DELETE", path, 200, NULL);
}

void test_users() {
    printf("\n📌 TEST MODULE: USERS (admin)\n");
    char email[64];
    snprintf(email, sizeof(email), "utilisateur_test_c_%s@example.com", timestamp_str);

    cJSON *user_data = cJSON_CreateObject();
    cJSON_AddStringToObject(user_data, "email", email);
    cJSON_AddStringToObject(user_data, "name", "Utilisateur de test C");
    cJSON_AddStringToObject(user_data, "password", "pass123");

    cJSON *user = api_call("admin", "POST", "/users", 201, user_data);
    int id = cJSON_GetObjectItem(user, "id")->valueint;
    cJSON_Delete(user_data);
    cJSON_Delete(user);

    cJSON *name_update = cJSON_CreateObject();
    cJSON_AddStringToObject(name_update, "name", "Tester Update C");
    char path[64];
    snprintf(path, sizeof(path), "/users/%d", id);
    api_call("admin", "PATCH", path, 200, name_update);
    cJSON_Delete(name_update);

    cJSON *get = api_call("admin", "GET", path, 200, NULL);
    assert_eq_str(get, "name", "Tester Update C");
    cJSON_Delete(get);

    api_call("admin", "DELETE", path, 200, NULL);
}

void test_orders() {
    printf("\n📌 TEST MODULE: ORDERS & ORDER ITEMS\n");
    char plant_name[64];
    snprintf(plant_name, sizeof(plant_name), "Plante_de_test_c_%s", timestamp_str);

    cJSON *plant_data = cJSON_CreateObject();
    cJSON_AddStringToObject(plant_data, "name", plant_name);
    cJSON_AddNumberToObject(plant_data, "price", 10);
    cJSON_AddNumberToObject(plant_data, "stock", 5);
    cJSON *plant = api_call("admin", "POST", "/admin/plants", 201, plant_data);
    int pid = cJSON_GetObjectItem(plant, "id")->valueint;
    cJSON_Delete(plant_data);
    cJSON_Delete(plant);

    cJSON *order_data = cJSON_CreateObject();
    cJSON *items_array = cJSON_AddArrayToObject(order_data, "items");
    cJSON *item = cJSON_CreateObject();
    cJSON_AddNumberToObject(item, "plantId", pid);
    cJSON_AddNumberToObject(item, "quantity", 2);
    cJSON_AddItemToArray(items_array, item);

    cJSON *order = api_call("user", "POST", "/orders", 201, order_data);
    int oid = cJSON_GetObjectItem(order, "id")->valueint;
    cJSON_Delete(order_data);
    cJSON_Delete(order);

    cJSON *status_update = cJSON_CreateObject();
    cJSON_AddStringToObject(status_update, "status", "shipped");
    char path[64];
    snprintf(path, sizeof(path), "/orders/%d", oid);
    api_call("admin", "PATCH", path, 200, status_update);
    cJSON_Delete(status_update);

    cJSON *list = api_call("user", "GET", "/orders", 200, NULL);
    cJSON *found = NULL;
    cJSON *o;
    cJSON_ArrayForEach(o, list) {
        if (cJSON_GetObjectItem(o, "id")->valueint == oid) {
            found = o;
            break;
        }
    }
    if (!found) { fprintf(stderr, "Commande non trouvée dans la liste\n"); exit(1); }
    assert_eq_str(found, "status", "shipped");
    cJSON_Delete(list);

    // Cleanup
    snprintf(path, sizeof(path), "/orders/%d", oid);
    api_call("admin", "DELETE", path, 200, NULL);
    snprintf(path, sizeof(path), "/admin/plants/%d", pid);
    api_call("admin", "DELETE", path, 200, NULL);
}

void test_auth_roles() {
    printf("\n📌 TEST MODULE: ROLES\n");
    cJSON *bad_plant = cJSON_CreateObject();
    cJSON_AddStringToObject(bad_plant, "name", "Bad Plant");
    cJSON_AddNumberToObject(bad_plant, "price", 1);
    cJSON_AddNumberToObject(bad_plant, "stock", 1);
    api_call("user", "POST", "/admin/plants", 403, bad_plant);
    cJSON_Delete(bad_plant);

    api_call("user", "GET", "/users", 403, NULL);
}

void test_auth_me(const char* user_email, const char* user_name) {
    printf("\n📌 TEST MODULE: AUTH /me\n");
    cJSON *me = api_call("user", "GET", "/auth/me", 200, NULL);
    assert_eq_str(me, "email", user_email);
    assert_eq_str(me, "name", user_name);
    printf("   ↳ Utilisateur connecté: %s (%s)\n", user_email, user_name);
    cJSON_Delete(me);
}


// ------------------------------------------------------
// 🚀 Exécution Principale
// ------------------------------------------------------
int main(void) {
    if (!waitForServer("127.0.0.1", 4100, 5000)) {
        fprintf(stderr, "❌ Serveur http://localhost:4100 injoignable.\n");
        return 2;
    }

    curl_global_init(CURL_GLOBAL_ALL);
    init_session(&admin_session);
    init_session(&user_session);

    // Génération du timestamp unique
    time_t t = time(NULL);
    struct tm *tm = localtime(&t);
    strftime(timestamp_str, sizeof(timestamp_str), "%Y%m%d%H%M%S", tm);

    char user_email[64];
    snprintf(user_email, sizeof(user_email), "user_c_%s@example.com", timestamp_str);
    const char* user_name = "User C";
    const char* user_password = "password123";

    printf("🧪 Démarrage des tests C: %s\n\n", BASE_URL);

    // --- Séquence d'initialisation ---
    cJSON *login_payload = cJSON_CreateObject();
    cJSON_AddStringToObject(login_payload, "email", ADMIN_EMAIL);
    cJSON_AddStringToObject(login_payload, "password", ADMIN_PASSWORD);
    api_call("admin", "POST", "/auth/login", 201, login_payload);
    cJSON_Delete(login_payload);

    cJSON *register_payload = cJSON_CreateObject();
    cJSON_AddStringToObject(register_payload, "name", user_name);
    cJSON_AddStringToObject(register_payload, "email", user_email);
    cJSON_AddStringToObject(register_payload, "password", user_password);
    api_call("user", "POST", "/auth/register", 201, register_payload);

    api_call("user", "POST", "/auth/login", 201, register_payload);
    cJSON_Delete(register_payload);

    // --- Exécution des tests ---
    test_plants();
    test_users();
    test_orders();
    test_auth_roles();
    test_auth_me(user_email, user_name);

    // --- Nettoyage ---
    cleanup_session(&admin_session);
    cleanup_session(&user_session);
    curl_global_cleanup();

    printf("\n🎉 Tous les tests C ont réussi!\n");
    return 0;
}
