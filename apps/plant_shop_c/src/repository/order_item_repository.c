/* ==============================================================================
   Importations
   ============================================================================== */
#include "order_item_repository.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Prépare les paramètres pour l insertion d un OrderItem.
 *
 * @param order_item OrderItem source
 * @param params Tableau de paramètres à remplir
 * @param buffers Tableau de buffers pour les conversions
 */
static void prepare_add_params(const OrderItem *order_item, const char **params, char buffers[4][12]) {
	sprintf(buffers[0], "%d", order_item->order_id);
	sprintf(buffers[1], "%d", order_item->plant_id);
	sprintf(buffers[2], "%d", order_item->qty);
	sprintf(buffers[3], "%d", order_item->price);
	for (int buffer_index = 0; buffer_index < 4; buffer_index++) params[buffer_index] = buffers[buffer_index];
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Ajoute un article de commande en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_item Pointeur vers l OrderItem à insérer
 */
void order_item_repo_add(PGconn *database_connection, const OrderItem *order_item) {
	const char *params[4];
	char buffers[4][12];
	prepare_add_params(order_item, params, buffers);
	PGresult *query_result = PQexecParams(database_connection, "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES($1,$2,$3,$4)", 4, NULL, params, NULL, NULL, 0);
	PQclear(query_result);
}

/**
 * Remplit un OrderItem depuis un résultat PostgreSQL.
 *
 * @param query_result Résultat de la requête
 * @param row_index Index de la ligne
 * @param order_item OrderItem à remplir
 */
static void fill_order_item(PGresult *query_result, int row_index, OrderItem *order_item) {
	order_item->id = atoi(PQgetvalue(query_result, row_index, 0));
	order_item->order_id = atoi(PQgetvalue(query_result, row_index, 1));
	order_item->plant_id = atoi(PQgetvalue(query_result, row_index, 2));
	order_item->qty = atoi(PQgetvalue(query_result, row_index, 3));
	order_item->price = atoi(PQgetvalue(query_result, row_index, 4));
}

/**
 * Récupère tous les articles d une commande via callback.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param callback_function Fonction callback appelée pour chaque article
 * @param callback_data Données utilisateur passées au callback
 */
void order_item_repo_by_order(PGconn *database_connection, int order_id, void(*callback_function)(OrderItem*, void*), void *callback_data) {
	char id_buffer[12];
	sprintf(id_buffer, "%d", order_id);
	const char *params[1] = {id_buffer};
	PGresult *query_result = PQexecParams(database_connection, "SELECT id,order_id,plant_id,quantity,price FROM order_items WHERE order_id=$1", 1, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return; }
	for (int row_index = 0; row_index < PQntuples(query_result); row_index++) {
		OrderItem order_item;
		fill_order_item(query_result, row_index, &order_item);
		callback_function(&order_item, callback_data);
	}
	PQclear(query_result);
}

/**
 * Met à jour un article de commande (quantité).
 *
 * @param database_connection Connexion PostgreSQL
 * @param item_id ID de l article
 * @param json_data Objet cJSON contenant les champs à modifier
 */
void order_item_repo_patch(PGconn* database_connection, int item_id, cJSON* json_data) {
	char id_string[12];
	sprintf(id_string, "%d", item_id);
	cJSON* quantity_value = cJSON_GetObjectItem(json_data, "quantity");
	if (quantity_value && cJSON_IsNumber(quantity_value)) {
		char quantity_string[12];
		sprintf(quantity_string, "%d", quantity_value->valueint);
		const char* params[2] = {quantity_string, id_string};
		PQclear(PQexecParams(database_connection, "UPDATE order_items SET quantity=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
	}
}

/**
 * Supprime un article de commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param item_id ID de l article à supprimer
 */
void order_item_repo_del(PGconn* database_connection, int item_id) {
	char id_string[12];
	sprintf(id_string, "%d", item_id);
	const char* params[1] = {id_string};
	PQclear(PQexecParams(database_connection, "DELETE FROM order_items WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}
