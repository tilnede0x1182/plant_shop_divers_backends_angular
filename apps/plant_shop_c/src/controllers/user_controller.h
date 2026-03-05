/**
 * @file user_controller.h
 * @brief Controller pour les operations sur les utilisateurs.
 */
#ifndef CTRL_USER_H
#define CTRL_USER_H

#include "mongoose/mongoose.h"

/**
 * Cree un nouvel utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees utilisateur
 */
void user_create(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Recupere les informations d'un utilisateur par son identifiant.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param user_identifier Identifiant de l'utilisateur
 */
void user_get(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Met a jour les informations d'un utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les nouvelles donnees
 * @param user_identifier Identifiant de l'utilisateur
 */
void user_patch(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Supprime un utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param user_identifier Identifiant de l'utilisateur a supprimer
 */
void user_del(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Liste tous les utilisateurs (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 */
void admin_users_list(struct mg_connection *c, struct mg_http_message *hm);

#endif
