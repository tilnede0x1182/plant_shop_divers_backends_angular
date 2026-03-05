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
 * @param plant_data Pointeur vers la structure Plant à remplir
 * @param query_result Résultat PostgreSQL
 * @param row_index Index de la ligne à lire
 */
static void fill_plant(Plant *plant_data, PGresult *query_result, int row_index) {
	plant_data->id = atoi(PQgetvalue(query_result, row_index, 0));
	strncpy(plant_data->name, PQgetvalue(query_result, row_index, 1), sizeof(plant_data->name) - 1);
	plant_data->name[sizeof(plant_data->name) - 1] = '\0';
	strncpy(plant_data->description, PQgetvalue(query_result, row_index, 2), sizeof(plant_data->description) - 1);
	plant_data->description[sizeof(plant_data->description) - 1] = '\0';
	plant_data->price = atoi(PQgetvalue(query_result, row_index, 3));
	plant_data->stock = atoi(PQgetvalue(query_result, row_index, 4));
}

/**
 * Ajoute une plante en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_data Pointeur vers la Plant à insérer
 * @return ID de la plante créée, 0 si erreur
 */
int plant_repo_add(PGconn *database_connection, const Plant *plant_data) {
	char price_str[12], stock_str[12];
	sprintf(price_str, "%.2f", (double)plant_data->price);
	sprintf(stock_str, "%d", plant_data->stock);
	const char *description = plant_data->description ? plant_data->description : "";
	const char *params[4] = {plant_data->name, description, price_str, stock_str};
	PGresult *query_result = PQexecParams(database_connection, "INSERT INTO plants(name,description,price,stock) VALUES($1,$2,$3,$4) RETURNING id", 4, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return 0; }
	int id = atoi(PQgetvalue(query_result, 0, 0));
	PQclear(query_result);
	return id;
}

/**
 * Recherche une plante par son ID.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_id ID de la plante
 * @param plant_data Pointeur vers la structure à remplir
 * @return 1 si trouvée, 0 sinon
 */
int plant_repo_find(PGconn *database_connection, int plant_id, Plant *plant_data) {
	char string_id[12];
	sprintf(string_id, "%d", plant_id);
	const char *params[1] = {string_id};
	PGresult *query_result = PQexecParams(database_connection, "SELECT id,name,description,price,stock FROM plants WHERE id=$1", 1, NULL, params, NULL, NULL, 0);
	int found = PQntuples(query_result);
	if (found) fill_plant(plant_data, query_result, 0);
	PQclear(query_result);
	return found;
}

/**
 * Met à jour le champ name d une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param string_id ID en string
 * @param json_data Objet JSON
 */
static void patch_name(PGconn *database_connection, const char *string_id, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "name");
	if (!json_value || !cJSON_IsString(json_value)) return;
	const char *params[2] = {json_value->valuestring, string_id};
	PQclear(PQexecParams(database_connection, "UPDATE plants SET name=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ description d une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param string_id ID en string
 * @param json_data Objet JSON
 */
static void patch_description(PGconn *database_connection, const char *string_id, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "description");
	if (!json_value || !cJSON_IsString(json_value)) return;
	const char *params[2] = {json_value->valuestring, string_id};
	PQclear(PQexecParams(database_connection, "UPDATE plants SET description=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ price d une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param string_id ID en string
 * @param json_data Objet JSON
 */
static void patch_price(PGconn *database_connection, const char *string_id, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "price");
	if (!json_value || !cJSON_IsNumber(json_value)) return;
	char price_str[12];
	sprintf(price_str, "%.2f", json_value->valuedouble);
	const char *params[2] = {price_str, string_id};
	PQclear(PQexecParams(database_connection, "UPDATE plants SET price=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ stock d une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param string_id ID en string
 * @param json_data Objet JSON
 */
static void patch_stock(PGconn *database_connection, const char *string_id, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "stock");
	if (!json_value || !cJSON_IsNumber(json_value)) return;
	char stock_str[12];
	sprintf(stock_str, "%d", json_value->valueint);
	const char *params[2] = {stock_str, string_id};
	PQclear(PQexecParams(database_connection, "UPDATE plants SET stock=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_id ID de la plante
 * @param json_data Objet JSON contenant les champs à modifier
 */
void plant_repo_patch(PGconn *database_connection, int plant_id, cJSON *json_data) {
	char string_id[12];
	sprintf(string_id, "%d", plant_id);
	patch_name(database_connection, string_id, json_data);
	patch_description(database_connection, string_id, json_data);
	patch_price(database_connection, string_id, json_data);
	patch_stock(database_connection, string_id, json_data);
}

/**
 * Supprime une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_id ID de la plante à supprimer
 */
void plant_repo_del(PGconn *database_connection, int plant_id) {
	char string_id[12];
	sprintf(string_id, "%d", plant_id);
	const char *params[1] = {string_id};
	PQclear(PQexecParams(database_connection, "DELETE FROM plants WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}

/**
 * Parcourt toutes les plantes via callback.
 *
 * @param conn Connexion PostgreSQL
 * @param callback_function Fonction callback appelée pour chaque plante
 * @param callback_context Données utilisateur passées au callback
 */
void plant_repo_each(PGconn *database_connection, void (*callback_function)(Plant*, void*), void *callback_context) {
	PGresult *query_result = PQexec(database_connection, "SELECT id,name,description,price,stock FROM plants ORDER BY name ASC");
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return; }
	for (int row_index = 0; row_index < PQntuples(query_result); row_index++) {
		Plant plant_data;
		fill_plant(&plant_data, query_result, row_index);
		callback_function(&plant_data, callback_context);
	}
	PQclear(query_result);
}
