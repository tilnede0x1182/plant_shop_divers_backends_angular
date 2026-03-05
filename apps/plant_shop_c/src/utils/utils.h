/**
 * @file utils.h
 * @brief Fonctions utilitaires pour la lecture de configuration et les cookies.
 */
#ifndef UTILS_H
#define UTILS_H

#include <stddef.h>
#include "../mongoose/mongoose.h"

/**
 * Lit les variables d'environnement pour la connexion a la base de donnees.
 *
 * @param database_url Buffer pour stocker l'URL de la base
 * @param database_user Buffer pour stocker le nom d'utilisateur
 * @param database_password Buffer pour stocker le mot de passe
 */
void read_db_env(char* url, char* user, char* pass);

/**
 * Lit les variables d'environnement pour la configuration du serveur.
 *
 * @param server_port Buffer pour stocker le port
 * @param jwt_secret_key Buffer pour stocker la cle secrete JWT
 */
void read_server_env(char* port, char* jwt_secret);

/**
 * Extrait la valeur d'un cookie depuis le message HTTP.
 *
 * @param http_message Message HTTP contenant les en-tetes
 * @param cookie_name Nom du cookie a extraire
 * @param output_buffer Buffer pour stocker la valeur
 * @param buffer_size Taille du buffer de sortie
 * @return 1 si trouve, 0 sinon
 */
int get_cookie_manual(struct mg_http_message* hm, const char* name, char* out, size_t sz);

/**
 * Recupere l'identifiant de l'utilisateur courant depuis le cookie de session.
 *
 * @param http_message Message HTTP contenant les en-tetes
 * @return Identifiant de l'utilisateur, ou 0 si non connecte
 */
int get_current_user_id(struct mg_http_message *hm);

#endif
