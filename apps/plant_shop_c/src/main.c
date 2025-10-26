#include <libpq-fe.h>
#include "mongoose/mongoose.h"
#include "routes.h"
#include "utils/utils.h"
#include <stdbool.h>

PGconn *DB = NULL;
char JWT_SECRET[128] = {0};

// Mongoose event handler function
static void fn(struct mg_connection *c, int ev, void *ev_data) {
	if (ev == MG_EV_HTTP_MSG) {
		struct mg_http_message *hm = (struct mg_http_message *) ev_data;
		route_request(c, hm);
	}
}

// Database connection
static void db_connect(void) {
    char db_url[128], db_user[64], db_pass[64];
    read_db_env(db_url, db_user, db_pass);

    char conn_str[512];
    snprintf(conn_str, sizeof(conn_str), "dbname=%s user=%s password=%s", db_url, db_user, db_pass);

    DB = PQconnectdb(conn_str);
    if (PQstatus(DB) != CONNECTION_OK) {
        printf("❌ Connexion à la base de données échouée : %s\n", PQerrorMessage(DB));
        PQfinish(DB);
        exit(1);
    }
    printf("✅ Connexion à la base de données '%s' réussie\n", db_url);
}

int main(void) {
    struct mg_mgr mgr;
    char port[16];
    char url[32];

    mg_log_set(MG_LL_NONE);

    db_connect();
    read_server_env(port, JWT_SECRET);
    snprintf(url, sizeof(url), "http://0.0.0.0:%s", port);

    mg_mgr_init(&mgr);
    printf("🚀 Démarrage de Mongoose v%s sur %s\n", MG_VERSION, url);

    struct mg_connection *lc = mg_http_listen(&mgr, url, fn, NULL);
    if (lc == NULL) {
        printf("❌ Port occupé ou droits insuffisants : %s\n", url);
        PQfinish(DB);
        return 1;
    }

    for (;;) {
        mg_mgr_poll(&mgr, 1000);
    }

    mg_mgr_free(&mgr);
    PQfinish(DB);
    return 0;
}
