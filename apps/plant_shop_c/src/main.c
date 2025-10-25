#include <kore/kore.h>
#include <kore/http.h>
#include <libpq-fe.h>
#include "../routes.h"
#include "utils/utils.h"

PGconn *DB = NULL;

/* -------- Connexion DB (corrigée) -------- */
static void db_connect(void){
    char db_url[128], db_user[64], db_pass[64];
    read_env(db_url, db_user, db_pass);

    char conn_str[512];
    snprintf(conn_str, sizeof(conn_str), "dbname=%s user=%s password=%s", db_url, db_user, db_pass);

    DB = PQconnectdb(conn_str);
    if (PQstatus(DB) != CONNECTION_OK) {
        kore_log(LOG_ERR, "DB connect failed: %s", PQerrorMessage(DB));
        PQfinish(DB);
        exit(1);
    }
    kore_log(LOG_INFO, "Successfully connected to database '%s'", db_url);
}

/* -------- init Kore (inchangé) -------- */
int init(int state){
    if (state == KORE_MODULE_LOAD) {
        db_connect();
        register_routes();
        kore_log(LOG_INFO, "Plant-Shop C ready");
    }
    return KORE_RESULT_OK;
}
