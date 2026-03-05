/**
 * @file order_item_repository.h
 * @brief Repository pour les operations CRUD sur les articles de commande.
 */
#ifndef REPO_ORDER_ITEM_H
#define REPO_ORDER_ITEM_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/order_item.h"

/**
 * Ajoute un article a une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_item_data Pointeur vers les donnees de l'article
 */
void order_item_repo_add(PGconn*, const OrderItem*);

/**
 * Parcourt les articles d'une commande et appelle le callback pour chacun.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_identifier Identifiant de la commande
 * @param callback_function Fonction appelee pour chaque article
 * @param callback_data Donnees utilisateur passees au callback
 */
void order_item_repo_by_order(PGconn*, int, void(*)(OrderItem*, void*), void*);

/**
 * Met a jour un article de commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param item_identifier Identifiant de l'article
 * @param json_data Donnees de mise a jour en JSON
 */
void order_item_repo_patch(PGconn*, int id, cJSON* data);

/**
 * Supprime un article de commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param item_identifier Identifiant de l'article a supprimer
 */
void order_item_repo_del(PGconn*, int id);

#endif
