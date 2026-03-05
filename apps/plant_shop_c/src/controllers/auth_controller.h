/**
 * @file auth_controller.h
 * @brief Controller pour l'authentification (login, register, logout, me).
 */
#ifndef CTRL_AUTH_H
#define CTRL_AUTH_H

#include "mongoose/mongoose.h"

/**
 * Connecte un utilisateur avec email/mot de passe.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les identifiants
 */
void auth_login(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Enregistre un nouvel utilisateur.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees d'inscription
 */
void auth_register(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Retourne les informations de l'utilisateur connecte.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant le cookie de session
 */
void auth_me(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Deconnecte l'utilisateur en supprimant le cookie de session.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP (non utilise)
 */
void auth_logout(struct mg_connection *c, struct mg_http_message *hm);

#endif
