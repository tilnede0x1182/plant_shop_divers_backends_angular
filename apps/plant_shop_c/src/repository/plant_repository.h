#ifndef REPO_PLANT_H
#define REPO_PLANT_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/plant.h"

/**
 * Ajoute une plante en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_data Pointeur vers la Plant à insérer
 * @return Identifiant de la plante créée, 0 si erreur
 */
int plant_repo_add(PGconn *database_connection, const Plant *plant_data);

/**
 * Recherche une plante par son identifiant.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_identifier Identifiant de la plante
 * @param output_plant Pointeur vers la structure à remplir
 * @return 1 si trouvée, 0 sinon
 */
int plant_repo_find(PGconn *database_connection, int plant_identifier, Plant *output_plant);

/**
 * Met à jour une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_identifier Identifiant de la plante
 * @param json_data Objet JSON contenant les champs à modifier
 */
void plant_repo_patch(PGconn *database_connection, int plant_identifier, cJSON *json_data);

/**
 * Supprime une plante.
 *
 * @param database_connection Connexion PostgreSQL
 * @param plant_identifier Identifiant de la plante à supprimer
 */
void plant_repo_del(PGconn *database_connection, int plant_identifier);

/**
 * Parcourt toutes les plantes via callback.
 *
 * @param database_connection Connexion PostgreSQL
 * @param callback_function Fonction callback appelée pour chaque plante
 * @param callback_context Données utilisateur passées au callback
 */
void plant_repo_each(PGconn *database_connection, void (*callback_function)(Plant*, void*), void *callback_context);

#endif
