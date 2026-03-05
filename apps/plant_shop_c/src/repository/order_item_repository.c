#include "order_item_repository.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * Ajoute un article de commande en base de données.
 *
 * @param db Connexion PostgreSQL
 * @param it Pointeur vers l'OrderItem à insérer
 */
void order_item_repo_add(PGconn *db, const OrderItem *it) {
    const char *params[4];
    char buf_order[12], buf_plant[12], buf_qty[12], buf_price[12];
    sprintf(buf_order, "%d", it->order_id);
    sprintf(buf_plant, "%d", it->plant_id);
    sprintf(buf_qty, "%d", it->qty);
    sprintf(buf_price, "%d", it->price);
    params[0] = buf_order;
    params[1] = buf_plant;
    params[2] = buf_qty;
    params[3] = buf_price;
    PGresult *r = PQexecParams(
        db,
        "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES($1,$2,$3,$4)",
        4, NULL, params, NULL, NULL, 0);
    if (PQresultStatus(r) != PGRES_COMMAND_OK) {
        // fprintf(stderr, "order_item_repo_add: %s\n", PQerrorMessage(db));
    }
    PQclear(r);
}

/**
 * Récupère tous les articles d'une commande via callback.
 *
 * @param db Connexion PostgreSQL
 * @param oid ID de la commande
 * @param cb Fonction callback appelée pour chaque article
 * @param ud Données utilisateur passées au callback
 */
void order_item_repo_by_order(PGconn *db, int oid, void(*cb)(OrderItem*, void*), void *ud) {
    char buf[12];
    sprintf(buf, "%d", oid);
    const char *p[1] = {buf};
    PGresult *r = PQexecParams(
        db,
        "SELECT id,order_id,plant_id,quantity,price FROM order_items WHERE order_id=$1",
        1, NULL, p, NULL, NULL, 0);
    if (PQresultStatus(r) != PGRES_TUPLES_OK) {
        // fprintf(stderr, "order_item_repo_by_order: %s\n", PQerrorMessage(db));
        PQclear(r);
        return;
    }
    for (int i = 0; i < PQntuples(r); i++) {
        OrderItem it = {
            .id = atoi(PQgetvalue(r, i, 0)),
            .order_id = atoi(PQgetvalue(r, i, 1)),
            .plant_id = atoi(PQgetvalue(r, i, 2)),
            .qty = atoi(PQgetvalue(r, i, 3)),
            .price = atoi(PQgetvalue(r, i, 4))
        };
        cb(&it, ud);
    }
    PQclear(r);
}

/**
 * Met à jour un article de commande (quantité).
 *
 * @param db Connexion PostgreSQL
 * @param id ID de l'article
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
 * @param id ID de l'article à supprimer
 */
void order_item_repo_del(PGconn* db, int id) {
    char id_str[12];
    sprintf(id_str, "%d", id);
    const char* params[1] = {id_str};
    PQclear(PQexecParams(db, "DELETE FROM order_items WHERE id=$1", 1, NULL, params, NULL, NULL, 0));
}
