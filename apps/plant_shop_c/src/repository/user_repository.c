/* ==============================================================================
   Importations
   ============================================================================== */
#include "user_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/**
 * Remplit une structure User depuis un résultat PostgreSQL.
 *
 * @param user_data Pointeur vers la structure User à remplir
 * @param query_result Résultat PostgreSQL
 * @param row_index Index de la ligne à lire
 */
static void fill_user(User *user_data, PGresult *query_result, int row_index) {
	user_data->id = atoi(PQgetvalue(query_result, row_index, 0));
	strncpy(user_data->name, PQgetvalue(query_result, row_index, 1), sizeof(user_data->name) - 1);
	user_data->name[sizeof(user_data->name) - 1] = '\0';
	strncpy(user_data->email, PQgetvalue(query_result, row_index, 2), sizeof(user_data->email) - 1);
	user_data->email[sizeof(user_data->email) - 1] = '\0';
	strncpy(user_data->password_hash, PQgetvalue(query_result, row_index, 3), sizeof(user_data->password_hash) - 1);
	user_data->password_hash[sizeof(user_data->password_hash) - 1] = '\0';
	user_data->is_admin = (PQgetvalue(query_result, row_index, 4)[0] == 't');
}

/**
 * Ajoute un utilisateur en base de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_data Pointeur vers le User à insérer
 * @return ID de l utilisateur créé, 0 si erreur
 */
int user_repo_add(PGconn *database_connection, const User *user_data) {
	const char* query_params[4] = {user_data->name, user_data->email, user_data->password_hash, user_data->is_admin ? "t" : "f"};
	PGresult *query_result = PQexecParams(database_connection, "INSERT INTO users (name, email, password_hash, is_admin) VALUES ($1, $2, $3, $4) RETURNING id", 4, NULL, query_params, NULL, NULL, 0);
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return 0; }
	int user_identifier = atoi(PQgetvalue(query_result, 0, 0));
	PQclear(query_result);
	return user_identifier;
}

/**
 * Recherche un utilisateur par son ID.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @param output_user Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find(PGconn *database_connection, int user_identifier, User *output_user) {
	char identifier_string[12];
	sprintf(identifier_string, "%d", user_identifier);
	const char *query_params[1] = {identifier_string};
	PGresult *query_result = PQexecParams(database_connection, "SELECT id, name, email, password_hash, is_admin FROM users WHERE id = $1", 1, NULL, query_params, NULL, NULL, 0);
	int found_count = PQntuples(query_result);
	if (found_count) fill_user(output_user, query_result, 0);
	PQclear(query_result);
	return found_count;
}

/**
 * Recherche un utilisateur par son email.
 *
 * @param database_connection Connexion PostgreSQL
 * @param email Adresse email recherchée
 * @param output_user Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find_by_mail(PGconn *database_connection, const char *email, User *output_user) {
	const char *query_params[1] = {email};
	PGresult *query_result = PQexecParams(database_connection, "SELECT id, name, email, password_hash, is_admin FROM users WHERE email = $1", 1, NULL, query_params, NULL, NULL, 0);
	int found_count = PQntuples(query_result);
	if (found_count) fill_user(output_user, query_result, 0);
	PQclear(query_result);
	return found_count;
}

/**
 * Vérifie si un utilisateur est administrateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @return 1 si admin, 0 sinon
 */
int user_repo_is_admin(PGconn *database_connection, int user_identifier) {
	if (user_identifier == 0) return 0;
	char identifier_string[12];
	sprintf(identifier_string, "%d", user_identifier);
	const char *query_params[1] = {identifier_string};
	PGresult *query_result = PQexecParams(database_connection, "SELECT is_admin FROM users WHERE id = $1", 1, NULL, query_params, NULL, NULL, 0);
	int is_admin_flag = (PQntuples(query_result) > 0 && PQgetvalue(query_result, 0, 0)[0] == 't');
	PQclear(query_result);
	return is_admin_flag;
}

/**
 * Met à jour le champ name d un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param identifier_string Identifiant en string
 * @param json_data JSON contenant les données
 */
static void patch_user_name(PGconn *database_connection, const char *identifier_string, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "name");
	if (!json_value || !cJSON_IsString(json_value)) return;
	const char *query_params[2] = {json_value->valuestring, identifier_string};
	PQclear(PQexecParams(database_connection, "UPDATE users SET name = $1 WHERE id = $2", 2, NULL, query_params, NULL, NULL, 0));
}

/**
 * Met à jour le champ email d un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param identifier_string Identifiant en string
 * @param json_data JSON contenant les données
 */
static void patch_user_email(PGconn *database_connection, const char *identifier_string, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "email");
	if (!json_value || !cJSON_IsString(json_value)) return;
	const char *query_params[2] = {json_value->valuestring, identifier_string};
	PQclear(PQexecParams(database_connection, "UPDATE users SET email = $1 WHERE id = $2", 2, NULL, query_params, NULL, NULL, 0));
}

/**
 * Met à jour le champ admin d un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param identifier_string Identifiant en string
 * @param json_data JSON contenant les données
 */
static void patch_user_admin(PGconn *database_connection, const char *identifier_string, cJSON *json_data) {
	cJSON *json_value = cJSON_GetObjectItem(json_data, "admin");
	if (!json_value || !cJSON_IsBool(json_value)) return;
	const char *query_params[2] = {cJSON_IsTrue(json_value) ? "t" : "f", identifier_string};
	PQclear(PQexecParams(database_connection, "UPDATE users SET is_admin = $1 WHERE id = $2", 2, NULL, query_params, NULL, NULL, 0));
}

/**
 * Met à jour un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur
 * @param json_data Objet JSON contenant les champs à modifier
 */
void user_repo_patch(PGconn *database_connection, int user_identifier, cJSON *json_data) {
	char identifier_string[12];
	sprintf(identifier_string, "%d", user_identifier);
	patch_user_name(database_connection, identifier_string, json_data);
	patch_user_email(database_connection, identifier_string, json_data);
	patch_user_admin(database_connection, identifier_string, json_data);
}

/**
 * Supprime un utilisateur.
 *
 * @param database_connection Connexion PostgreSQL
 * @param user_identifier Identifiant de l utilisateur à supprimer
 */
void user_repo_del(PGconn *database_connection, int user_identifier) {
	char identifier_string[12];
	sprintf(identifier_string, "%d", user_identifier);
	const char *query_params[1] = {identifier_string};
	PQclear(PQexecParams(database_connection, "DELETE FROM users WHERE id = $1", 1, NULL, query_params, NULL, NULL, 0));
}

/**
 * Parcourt tous les utilisateurs via callback.
 *
 * @param database_connection Connexion PostgreSQL
 * @param callback_function Fonction callback appelée pour chaque utilisateur
 * @param callback_context Données utilisateur passées au callback
 */
void user_repo_each(PGconn *database_connection, void (*callback_function)(User*, void*), void *callback_context) {
	PGresult *query_result = PQexec(database_connection, "SELECT id, name, email, password_hash, is_admin FROM users");
	if (PQresultStatus(query_result) != PGRES_TUPLES_OK) { PQclear(query_result); return; }
	for (int row_index = 0; row_index < PQntuples(query_result); row_index++) {
		User user_data;
		fill_user(&user_data, query_result, row_index);
		callback_function(&user_data, callback_context);
	}
	PQclear(query_result);
}
