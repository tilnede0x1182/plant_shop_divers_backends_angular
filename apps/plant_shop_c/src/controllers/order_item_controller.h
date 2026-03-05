/**
 * @file order_item_controller.h
 * @brief Controller pour les operations sur les articles de commande.
 */
#ifndef CTRL_ORDER_ITEM_H
#define CTRL_ORDER_ITEM_H

#include "mongoose/mongoose.h"

/**
 * Liste les articles d'une commande specifique.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param order_identifier Identifiant de la commande
 */
void order_items_by_order(struct mg_connection *c, struct mg_http_message *hm, int order_id);

/**
 * Met a jour un article de commande.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les nouvelles donnees
 * @param item_identifier Identifiant de l'article
 */
void order_item_patch(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Supprime un article de commande.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param item_identifier Identifiant de l'article a supprimer
 */
void order_item_del(struct mg_connection *c, struct mg_http_message *hm, int id);

#endif
