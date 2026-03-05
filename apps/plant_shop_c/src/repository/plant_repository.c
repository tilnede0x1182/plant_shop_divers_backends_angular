#include "plant_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/**
 * Remplit une structure Plant depuis un résultat PostgreSQL.
 *
 * @param p Pointeur vers la structure Plant à remplir
 * @param r Résultat PostgreSQL
 * @param row Index de la ligne à lire
 */
static void fill_plant(Plant *p, PGresult *r, int row) {
	p->id = atoi(PQgetvalue(r, row, 0));
	strncpy(p->name, PQgetvalue(r, row, 1), sizeof(p->name) - 1);
	p->name[sizeof(p->name) - 1] = '\0';
	strncpy(p->description, PQgetvalue(r, row, 2), sizeof(p->description) - 1);
	p->description[sizeof(p->description) - 1] = '\0';
	p->price = atoi(PQgetvalue(r, row, 3));
	p->stock = atoi(PQgetvalue(r, row, 4));
}

/**
 * Ajoute une plante en base de données.
 *
 * @param c Connexion PostgreSQL
 * @param p Pointeur vers la Plant à insérer
 * @return ID de la plante créée, 0 si erreur
 */
int plant_repo_add(PGconn *c, const Plant *p) {
	char price_str[12], stock_str[12];
	sprintf(price_str, "%.2f", (double)p->price);
	sprintf(stock_str, "%d", p->stock);
	const char *desc = p->description ? p->description : "";
	const char *v[4] = {p->name, desc, price_str, stock_str};

	PGresult *r = PQexecParams(
			c,
			"INSERT INTO plants(name,description,price,stock) VALUES($1,$2,$3,$4) RETURNING id",
			4, NULL, v, NULL, NULL, 0);

	if (PQresultStatus(r) != PGRES_TUPLES_OK) {
		// fprintf(stderr, "plant_repo_add failed: %s\n", PQerrorMessage(c));
		PQclear(r);
		return 0;
	}
	int id = atoi(PQgetvalue(r, 0, 0));
	PQclear(r);
	return id;
}

/**
 * Recherche une plante par son ID.
 *
 * @param c Connexion PostgreSQL
 * @param id ID de la plante
 * @param p Pointeur vers la structure à remplir
 * @return 1 si trouvée, 0 sinon
 */
int plant_repo_find(PGconn *c, int id, Plant *p) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *v[1] = {sid};

	PGresult *r = PQexecParams(
		c,
		"SELECT id,name,description,price,stock FROM plants WHERE id=$1",
		1, NULL, v, NULL, NULL, 0);

	int found = PQntuples(r);
	if (found) fill_plant(p, r, 0);
	PQclear(r);
	return found;
}

/**
 * Met à jour une plante (name, description, price, stock).
 *
 * @param c Connexion PostgreSQL
 * @param id ID de la plante
 * @param j Objet JSON contenant les champs à modifier
 */
void plant_repo_patch(PGconn *c, int id, cJSON *j) {
    char sid[12];
    sprintf(sid, "%d", id);

    cJSON *name_json = cJSON_GetObjectItem(j, "name");
    if (name_json && cJSON_IsString(name_json)) {
        const char *p[2] = {name_json->valuestring, sid};
        PGresult *res = PQexecParams(
            c, "UPDATE plants SET name=$1 WHERE id=$2",
            2, NULL, p, NULL, NULL, 0);
        PQclear(res);
    }

    cJSON *desc_json = cJSON_GetObjectItem(j, "description");
    if (desc_json && cJSON_IsString(desc_json)) {
        const char *p[2] = {desc_json->valuestring, sid};
        PGresult *res = PQexecParams(
            c, "UPDATE plants SET description=$1 WHERE id=$2",
            2, NULL, p, NULL, NULL, 0);
        PQclear(res);
    }

    cJSON *price_json = cJSON_GetObjectItem(j, "price");
    if (price_json && cJSON_IsNumber(price_json)) {
        char price_str[12];
        sprintf(price_str, "%.2f", (double)price_json->valuedouble);
        const char *p[2] = {price_str, sid};
        PGresult *res = PQexecParams(
            c, "UPDATE plants SET price=$1 WHERE id=$2",
            2, NULL, p, NULL, NULL, 0);
        PQclear(res);
    }

    cJSON *stock_json = cJSON_GetObjectItem(j, "stock");
    if (stock_json && cJSON_IsNumber(stock_json)) {
        char stock_str[12];
        sprintf(stock_str, "%d", stock_json->valueint);
        const char *p[2] = {stock_str, sid};
        PGresult *res = PQexecParams(
            c, "UPDATE plants SET stock=$1 WHERE id=$2",
            2, NULL, p, NULL, NULL, 0);
        PQclear(res);
    }
}

/**
 * Supprime une plante.
 *
 * @param c Connexion PostgreSQL
 * @param id ID de la plante à supprimer
 */
void plant_repo_del(PGconn *c, int id) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *v[1] = {sid};

	PGresult *res = PQexecParams(
		c, "DELETE FROM plants WHERE id=$1",
		1, NULL, v, NULL, NULL, 0);
	PQclear(res);
}

/**
 * Parcourt toutes les plantes via callback.
 *
 * @param c Connexion PostgreSQL
 * @param cb Fonction callback appelée pour chaque plante
 * @param ctx Données utilisateur passées au callback
 */
void plant_repo_each(PGconn *c, void (*cb)(Plant*, void*), void *ctx) {
	PGresult *r = PQexec(c, "SELECT id,name,description,price,stock FROM plants ORDER BY name ASC");
	if (PQresultStatus(r) != PGRES_TUPLES_OK) {
		// fprintf(stderr, "plant_repo_each failed: %s\n", PQerrorMessage(c));
		PQclear(r);
		return;
	}

	int rows = PQntuples(r);
	for (int i = 0; i < rows; i++) {
		Plant p;
		fill_plant(&p, r, i);
		cb(&p, ctx);
	}
	PQclear(r);
}
