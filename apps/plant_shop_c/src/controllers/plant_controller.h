/**
 * @file plant_controller.h
 * @brief Controller pour les operations sur les plantes.
 */
#ifndef CTRL_PLANT_H
#define CTRL_PLANT_H

#include "mongoose/mongoose.h"

/**
 * Recupere les details d'une plante par son identifiant.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param plant_identifier Identifiant de la plante
 */
void plant_get(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Liste toutes les plantes (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 */
void admin_plants_list(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Ajoute une nouvelle plante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les donnees de la plante
 */
void admin_plants_add(struct mg_connection *c, struct mg_http_message *hm);

/**
 * Met a jour une plante existante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP contenant les nouvelles donnees
 * @param plant_identifier Identifiant de la plante
 */
void admin_plants_patch(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Supprime une plante (acces admin).
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 * @param plant_identifier Identifiant de la plante a supprimer
 */
void admin_plants_del(struct mg_connection *c, struct mg_http_message *hm, int id);

/**
 * Liste les plantes disponibles publiquement.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP
 */
void plants_list_public(struct mg_connection *c, struct mg_http_message *hm);

#endif
