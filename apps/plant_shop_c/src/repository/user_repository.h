#ifndef REPO_USER_H
#define REPO_USER_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/user.h"

/**
 * Ajoute un utilisateur en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_data Pointeur vers le User à insérer
 * @return Identifiant de l utilisateur créé, 0 si erreur
 */
int user_repo_add(PGconn *database_connection, const User *user_data);

/**
 * Recherche un utilisateur par son identifiant.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @param output_user Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find(PGconn *database_connection, int user_identifier, User *output_user);

/**
 * Recherche un utilisateur par son email.
 *
 * @param database_connection Connexion PostgreSQL
 * @param email Adresse email recherchée
 * @param output_user Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find_by_mail(PGconn *database_connection, const char *email, User *output_user);

/**
 * Met à jour un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @param json_data Objet JSON contenant les champs à modifier
 */
void user_repo_patch(PGconn *database_connection, int user_identifier, cJSON *json_data);

/**
 * Supprime un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur à supprimer
 */
void user_repo_del(PGconn *database_connection, int user_identifier);

/**
 * Parcourt tous les utilisateurs via callback.
 *
 * @param database_connection Connexion PostgreSQL
 * @param callback_function Fonction callback appelée pour chaque utilisateur
 * @param callback_context Données utilisateur passées au callback
 */
void user_repo_each(PGconn *database_connection, void (*callback_function)(User*, void*), void *callback_context);

/**
 * Vérifie si un utilisateur est administrateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @return 1 si admin, 0 sinon
 */
int user_repo_is_admin(PGconn *database_connection, int user_identifier);

#endif
