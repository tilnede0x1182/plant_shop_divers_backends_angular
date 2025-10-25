/** """ Point d’entrée Kore – bootstrap minimal
	@db_connect  ouvre PostgreSQL
	@init        charge routes via register_routes (routes.c)                 """ */

#include <kore/kore.h>
#include <kore/http.h>
#include <libpq-fe.h>
#include "routes.h"              /* prototype register_routes() */

static PGconn *DB = NULL;

/* -------- Connexion DB (≤15 lignes) -------- */
static void db_connect(void){
	const char *url  = kore_config_get_string("db_url");
	const char *user = kore_config_get_string("db_user");
	const char *pass = kore_config_get_string("db_pass");
	const char *k[]  = {"dbname","user","password",NULL};
	const char *v[]  = {url,user,pass,NULL};
	DB = PQconnectdbParams(k,v,0);
	if(PQstatus(DB)!=CONNECTION_OK){
		kore_log(LOG_ERR,"DB connect failed: %s",PQerrorMessage(DB));
		exit(1);
	}
}

/* -------- init Kore (≤15 lignes) -------- */
int init(int state){
	if(state==KORE_MODULE_LOAD){
		db_connect();
		register_routes();        /* ajouté dans routes.c */
		kore_log(LOG_INFO,"Plant-Shop C ready");
	}
	return KORE_RESULT_OK;
}
