/* ==============================================================================
   Importations
   ============================================================================== */
#include <libpq-fe.h>
#include "mongoose/mongoose.h"
#include "routes.h"
#include "utils/utils.h"
#include <stdbool.h>

/* ==============================================================================
   Données
   ============================================================================== */
PGconn *DATABASE_CONNECTION = NULL;
char JWT_SECRET[128] = {0};

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Gestionnaire d événements Mongoose.
 * Traite les messages HTTP entrants.
 *
 * @param connection Connexion Mongoose
 * @param event Type d événement
 * @param event_data Données de l événement
 */
static void http_event_handler(struct mg_connection *connection, int event, void *event_data) {
	if (event == MG_EV_HTTP_MSG) {
		struct mg_http_message *http_message = (struct mg_http_message *) event_data;
		route_request(connection, http_message);
	}
}

/**
 * Établit la connexion à la base de données PostgreSQL.
 * Lit les paramètres depuis les variables d environnement.
 */
static void database_connect(void) {
    char database_url[128], database_user[64], database_password[64];
    read_db_env(database_url, database_user, database_password);

    char connection_string[512];
    snprintf(connection_string, sizeof(connection_string), "dbname=%s user=%s password=%s", database_url, database_user, database_password);

    DATABASE_CONNECTION = PQconnectdb(connection_string);
    if (PQstatus(DATABASE_CONNECTION) != CONNECTION_OK) {
        printf("❌ Connexion à la base de données échouée : %s\n", PQerrorMessage(DATABASE_CONNECTION));
        PQfinish(DATABASE_CONNECTION);
        exit(1);
    }
    printf("✅ Connexion à la base de données '%s' réussie\n", database_url);
}

/**
 * Initialise le manager Mongoose et démarre l écoute.
 *
 * @param mongoose_manager Pointeur vers le manager Mongoose
 * @param listen_url URL d écoute
 * @return 1 si succès, 0 si erreur
 */
static int start_server(struct mg_mgr* mongoose_manager, const char* listen_url) {
	mg_mgr_init(mongoose_manager);
	printf("🚀 Démarrage de Mongoose v%s sur %s\n", MG_VERSION, listen_url);
	struct mg_connection *listen_connection = mg_http_listen(mongoose_manager, listen_url, http_event_handler, NULL);
	if (listen_connection == NULL) { printf("❌ Port occupé ou droits insuffisants : %s\n", listen_url); return 0; }
	return 1;
}

/**
 * Boucle principale du serveur.
 *
 * @param mongoose_manager Pointeur vers le manager Mongoose
 */
static void run_server_loop(struct mg_mgr* mongoose_manager) {
	for (;;) mg_mgr_poll(mongoose_manager, 1000);
}

/* ==============================================================================
   Main
   ============================================================================== */
/**
 * Point d entrée principal du serveur.
 * Initialise la connexion DB et démarre le serveur HTTP Mongoose.
 *
 * @return Code de sortie (0 = succès)
 */
int main(void) {
	struct mg_mgr mongoose_manager;
	char port[16], server_url[32];
	mg_log_set(MG_LL_NONE);
	database_connect();
	read_server_env(port, JWT_SECRET);
	snprintf(server_url, sizeof(server_url), "http://0.0.0.0:%s", port);
	if (!start_server(&mongoose_manager, server_url)) { PQfinish(DATABASE_CONNECTION); return 1; }
	run_server_loop(&mongoose_manager);
	mg_mgr_free(&mongoose_manager);
	PQfinish(DATABASE_CONNECTION);
	return 0;
}
