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
 * @param it OrderItem source
 * @param params Tableau de paramètres à remplir
 * @param bufs Tableau de buffers pour les conversions
 */
static void prepare_add_params(const OrderItem *it, const char **params, char bufs[4][12]) {
	sprintf(bufs[0], "%d", it->order_id);
	sprintf(bufs[1], "%d", it->plant_id);
	sprintf(bufs[2], "%d", it->qty);
	sprintf(bufs[3], "%d", it->price);
	for (int idx = 0; idx < 4; idx++) params[idx] = bufs[idx];
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Ajoute un article de commande en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param it Pointeur vers l OrderItem à insérer
 */
void order_item_repo_add(PGconn *db, const OrderItem *it) {
	const char *params[4];
	char bufs[4][12];
	prepare_add_params(it, params, bufs);
	PGresult *res = PQexecParams(db, "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES($1,$2,$3,$4)", 4, NULL, params, NULL, NULL, 0);
	PQclear(res);
}

/**
 * Remplit un OrderItem depuis un résultat PostgreSQL.
 *
 * @param res Résultat de la requête
 * @param row Index de la ligne
 * @param it OrderItem à remplir
 */
static void fill_order_item(PGresult *res, int row, OrderItem *it) {
	it->id = atoi(PQgetvalue(res, row, 0));
	it->order_id = atoi(PQgetvalue(res, row, 1));
	it->plant_id = atoi(PQgetvalue(res, row, 2));
	it->qty = atoi(PQgetvalue(res, row, 3));
	it->price = atoi(PQgetvalue(res, row, 4));
}

/**
 * Récupère tous les articles d une commande via callback.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param callback_function Fonction callback appelée pour chaque article
 * @param callback_data Données utilisateur passées au callback
 */
void order_item_repo_by_order(PGconn *db, int order_id, void(*cb)(OrderItem*, void*), void *ud) {
	char buf[12];
	sprintf(buf, "%d", order_id);
	const char *params[1] = {buf};
	PGresult *res = PQexecParams(db, "SELECT id,order_id,plant_id,quantity,price FROM order_items WHERE order_id=$1", 1, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return; }
	for (int idx = 0; idx < PQntuples(res); idx++) {
		OrderItem it;
		fill_order_item(res, idx, &it);
		cb(&it, ud);
	}
	PQclear(res);
}

/**
 * Met à jour un article de commande (quantité).
 *
 * @param db Connexion PostgreSQL
 * @param id ID de l article
 * @param data Objet cJSON contenant les champs à modifier
 */
void order_item_repo_patch(PGconn* db, int id, cJSON* data) {
	char id_str[12];
	sprintf(id_str, "%d", id);
	cJSON* qty = cJSON_GetObjectItem(data, "quantity");
	if (qty && cJSON_IsNumber(qty)) {
		char qty_str[12];
		sprintf(qty_str, "%d", qty->valueint);
		const char* params[2] = {qty_str, id_str};
		PQclear(PQexecParams(db, "UPDATE order_items SET quantity=$1 WHERE id=$2", 2, NULL, params, NULL, NULL, 0));
	}
}

/**
 * Supprime un article de commande.
 *
 * @param db Connexion PostgreSQL
 * @param id ID de l article à supprimer
 */
void order_item_repo_del(PGconn* db, int id) {
	char id_str[12];
	sprintf(id_str, "%d", id);
	const char* params[1] = {id_str};
	PQclear(PQexecParams(db, "DELETE FROM order_items WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}
