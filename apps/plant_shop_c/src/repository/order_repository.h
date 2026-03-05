/**
 * @file order_repository.h
 * @brief Repository pour les operations CRUD sur les commandes.
 */
#ifndef REPO_ORDER_H
#define REPO_ORDER_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/order.h"

/**
 * Cree une nouvelle commande pour un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l'utilisateur
 * @param items_json Liste des articles en JSON
 * @return Identifiant de la commande creee, ou -1 si erreur
 */
int order_repo_add(PGconn*, int user_id, cJSON* items);

/**
 * Liste toutes les commandes d'un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l'utilisateur
 * @return Tableau JSON des commandes
 */
cJSON* order_repo_list(PGconn*, int user_id);

/**
 * Met a jour une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_identifier Identifiant de la commande
 * @param json_data Donnees de mise a jour en JSON
 */
void order_repo_patch(PGconn*, int id, cJSON* data);

/**
 * Supprime une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_identifier Identifiant de la commande a supprimer
 */
void order_repo_del(PGconn*, int id);

/**
 * Verifie si une commande appartient a un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_identifier Identifiant de la commande
 * @param user_identifier Identifiant de l'utilisateur
 * @return 1 si vrai, 0 sinon
 */
int order_repo_belongs_to(PGconn*, int order_id, int user_id);

/**
 * Verifie si un utilisateur est administrateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l'utilisateur
 * @return 1 si admin, 0 sinon
 */
int order_repo_is_admin(PGconn *db, int user_id);

/**
 * Met a jour le statut d'une commande.
 *
 * @param database_connection Connexion PostgreSQL
 * @param order_identifier Identifiant de la commande
 * @param status_string Nouveau statut
 * @return 1 si succes, 0 sinon
 */
int order_repo_update_status(PGconn *conn, int order_id, const char* status);

#endif
