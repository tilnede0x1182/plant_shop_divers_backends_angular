/**
 * @file order_controller.h
 * @brief Controller pour les operations sur les commandes.
 */
#ifndef CTRL_ORDER_H
#define CTRL_ORDER_H

#include "mongoose/mongoose.h"

/**
 * Liste les commandes de l'utilisateur connecte.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant le cookie de session
 */
void orders_list(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Cree une nouvelle commande pour l'utilisateur connecte.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les articles
 */
void orders_create(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Met a jour une commande existante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les nouvelles donnees
 * @param order_identifier Identifiant de la commande
 */
void orders_patch(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Supprime une commande (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param order_identifier Identifiant de la commande a supprimer
 */
void orders_del(struct mg_connection *c, struct mg_http_message *hm, int id);

#endif
