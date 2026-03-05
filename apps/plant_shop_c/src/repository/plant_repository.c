/* ==============================================================================
   Importations
   ============================================================================== */
#include "plant_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/**
 * Remplit une structure Plant depuis un résultat PostgreSQL.
 *
 * @param pl Pointeur vers la structure Plant à remplir
 * @param res Résultat PostgreSQL
 * @param row Index de la ligne à lire
 */
static void fill_plant(Plant *pl, PGresult *res, int row) {
	pl->id = atoi(PQgetvalue(res, row, 0));
	strncpy(pl->name, PQgetvalue(res, row, 1), sizeof(pl->name) - 1);
	pl->name[sizeof(pl->name) - 1] = '\0';
	strncpy(pl->description, PQgetvalue(res, row, 2), sizeof(pl->description) - 1);
	pl->description[sizeof(pl->description) - 1] = '\0';
	pl->price = atoi(PQgetvalue(res, row, 3));
	pl->stock = atoi(PQgetvalue(res, row, 4));
}

/**
 * Ajoute une plante en base de données.
 *
 * @param conn Connexion PostgreSQL
 * @param pl Pointeur vers la Plant à insérer
 * @return ID de la plante créée, 0 si erreur
 */
int plant_repo_add(PGconn *conn, const Plant *pl) {
	char price_str[12], stock_str[12];
	sprintf(price_str, "%.2f", (double)pl->price);
	sprintf(stock_str, "%d", pl->stock);
	const char *desc = pl->description ? pl->description : "";
	const char *params[4] = {pl->name, desc, price_str, stock_str};
	PGresult *res = PQexecParams(conn, "INSERT INTO plants(name,description,price,stock) VALUES($1,$2,$3,$4) RETURNING id", 4, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return 0; }
	int id = atoi(PQgetvalue(res, 0, 0));
	PQclear(res);
	return id;
}

/**
 * Recherche une plante par son ID.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de la plante
 * @param pl Pointeur vers la structure à remplir
 * @return 1 si trouvée, 0 sinon
 */
int plant_repo_find(PGconn *conn, int id, Plant *pl) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *params[1] = {sid};
	PGresult *res = PQexecParams(conn, "SELECT id,name,description,price,stock FROM plants WHERE id=$1", 1, NULL, params, NULL, NULL, 0);
	int found = PQntuples(res);
	if (found) fill_plant(pl, res, 0);
	PQclear(res);
	return found;
}

/**
 * Met à jour le champ name d une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param sid ID en string
 * @param json Objet JSON
 */
static void patch_name(PGconn *conn, const char *sid, cJSON *json) {
	cJSON *val = cJSON_GetObjectItem(json, "name");
	if (!val || !cJSON_IsString(val)) return;
	const char *params[2] = {val->valuestring, sid};
	PQclear(PQexecParams(conn, "UPDATE plants SET name=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ description d une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param sid ID en string
 * @param json Objet JSON
 */
static void patch_description(PGconn *conn, const char *sid, cJSON *json) {
	cJSON *val = cJSON_GetObjectItem(json, "description");
	if (!val || !cJSON_IsString(val)) return;
	const char *params[2] = {val->valuestring, sid};
	PQclear(PQexecParams(conn, "UPDATE plants SET description=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ price d une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param sid ID en string
 * @param json Objet JSON
 */
static void patch_price(PGconn *conn, const char *sid, cJSON *json) {
	cJSON *val = cJSON_GetObjectItem(json, "price");
	if (!val || !cJSON_IsNumber(val)) return;
	char price_str[12];
	sprintf(price_str, "%.2f", val->valuedouble);
	const char *params[2] = {price_str, sid};
	PQclear(PQexecParams(conn, "UPDATE plants SET price=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ stock d une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param sid ID en string
 * @param json Objet JSON
 */
static void patch_stock(PGconn *conn, const char *sid, cJSON *json) {
	cJSON *val = cJSON_GetObjectItem(json, "stock");
	if (!val || !cJSON_IsNumber(val)) return;
	char stock_str[12];
	sprintf(stock_str, "%d", val->valueint);
	const char *params[2] = {stock_str, sid};
	PQclear(PQexecParams(conn, "UPDATE plants SET stock=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de la plante
 * @param json Objet JSON contenant les champs à modifier
 */
void plant_repo_patch(PGconn *conn, int id, cJSON *json) {
	char sid[12];
	sprintf(sid, "%d", id);
	patch_name(conn, sid, json);
	patch_description(conn, sid, json);
	patch_price(conn, sid, json);
	patch_stock(conn, sid, json);
}

/**
 * Supprime une plante.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de la plante à supprimer
 */
void plant_repo_del(PGconn *conn, int id) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *params[1] = {sid};
	PQclear(PQexecParams(conn, "DELETE FROM plants WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}

/**
 * Parcourt toutes les plantes via callback.
 *
 * @param conn Connexion PostgreSQL
 * @param cb Fonction callback appelée pour chaque plante
 * @param ctx Données utilisateur passées au callback
 */
void plant_repo_each(PGconn *conn, void (*cb)(Plant*, void*), void *ctx) {
	PGresult *res = PQexec(conn, "SELECT id,name,description,price,stock FROM plants ORDER BY name ASC");
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return; }
	for (int idx = 0; idx < PQntuples(res); idx++) {
		Plant pl;
		fill_plant(&pl, res, idx);
		cb(&pl, ctx);
	}
	PQclear(res);
}
