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
 * @param usr Pointeur vers la structure User à remplir
 * @param res Résultat PostgreSQL
 * @param row Index de la ligne à lire
 */
static void fill_user(User *usr, PGresult *res, int row) {
	usr->id = atoi(PQgetvalue(res, row, 0));
	strncpy(usr->name, PQgetvalue(res, row, 1), sizeof(usr->name) - 1);
	usr->name[sizeof(usr->name) - 1] = '\0';
	strncpy(usr->email, PQgetvalue(res, row, 2), sizeof(usr->email) - 1);
	usr->email[sizeof(usr->email) - 1] = '\0';
	strncpy(usr->password_hash, PQgetvalue(res, row, 3), sizeof(usr->password_hash) - 1);
	usr->password_hash[sizeof(usr->password_hash) - 1] = '\0';
	usr->is_admin = (PQgetvalue(res, row, 4)[0] == 't');
}

/**
 * Ajoute un utilisateur en base de données.
 *
 * @param conn Connexion PostgreSQL
 * @param usr Pointeur vers le User à insérer
 * @return ID de l utilisateur créé, 0 si erreur
 */
int user_repo_add(PGconn *conn, const User *usr) {
	const char* params[4] = {usr->name, usr->email, usr->password_hash, usr->is_admin ? "t" : "f"};
	PGresult *res = PQexecParams(conn, "INSERT INTO users (name, email, password_hash, is_admin) VALUES ($1, $2, $3, $4) RETURNING id", 4, NULL, params, NULL, NULL, 0);
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return 0; }
	int id = atoi(PQgetvalue(res, 0, 0));
	PQclear(res);
	return id;
}

/**
 * Recherche un utilisateur par son ID.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l utilisateur
 * @param out Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find(PGconn *conn, int id, User *out) {
	char id_str[12];
	sprintf(id_str, "%d", id);
	const char *params[1] = {id_str};
	PGresult *res = PQexecParams(conn, "SELECT id, name, email, password_hash, is_admin FROM users WHERE id = $1", 1, NULL, params, NULL, NULL, 0);
	int found = PQntuples(res);
	if (found) fill_user(out, res, 0);
	PQclear(res);
	return found;
}

/**
 * Recherche un utilisateur par son email.
 *
 * @param conn Connexion PostgreSQL
 * @param email Adresse email recherchée
 * @param out Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find_by_mail(PGconn *conn, const char *email, User *out) {
	const char *params[1] = {email};
	PGresult *res = PQexecParams(conn, "SELECT id, name, email, password_hash, is_admin FROM users WHERE email = $1", 1, NULL, params, NULL, NULL, 0);
	int found = PQntuples(res);
	if (found) fill_user(out, res, 0);
	PQclear(res);
	return found;
}

/**
 * Vérifie si un utilisateur est administrateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l utilisateur
 * @return 1 si admin, 0 sinon
 */
int user_repo_is_admin(PGconn *conn, int id) {
	if (id == 0) return 0;
	char id_str[12];
	sprintf(id_str, "%d", id);
	const char *params[1] = {id_str};
	PGresult *res = PQexecParams(conn, "SELECT is_admin FROM users WHERE id = $1", 1, NULL, params, NULL, NULL, 0);
	int is_admin = (PQntuples(res) > 0 && PQgetvalue(res, 0, 0)[0] == 't');
	PQclear(res);
	return is_admin;
}

/**
 * Met à jour le champ name d un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id_str ID en string
 * @param data JSON contenant les données
 */
static void patch_user_name(PGconn *conn, const char *id_str, cJSON *data) {
	cJSON *val = cJSON_GetObjectItem(data, "name");
	if (!val || !cJSON_IsString(val)) return;
	const char *params[2] = {val->valuestring, id_str};
	PQclear(PQexecParams(conn, "UPDATE users SET name = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ email d un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id_str ID en string
 * @param data JSON contenant les données
 */
static void patch_user_email(PGconn *conn, const char *id_str, cJSON *data) {
	cJSON *val = cJSON_GetObjectItem(data, "email");
	if (!val || !cJSON_IsString(val)) return;
	const char *params[2] = {val->valuestring, id_str};
	PQclear(PQexecParams(conn, "UPDATE users SET email = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour le champ admin d un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id_str ID en string
 * @param data JSON contenant les données
 */
static void patch_user_admin(PGconn *conn, const char *id_str, cJSON *data) {
	cJSON *val = cJSON_GetObjectItem(data, "admin");
	if (!val || !cJSON_IsBool(val)) return;
	const char *params[2] = {cJSON_IsTrue(val) ? "t" : "f", id_str};
	PQclear(PQexecParams(conn, "UPDATE users SET is_admin = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
}

/**
 * Met à jour un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l utilisateur
 * @param data Objet JSON contenant les champs à modifier
 */
void user_repo_patch(PGconn *conn, int id, cJSON *data) {
	char id_str[12];
	sprintf(id_str, "%d", id);
	patch_user_name(conn, id_str, data);
	patch_user_email(conn, id_str, data);
	patch_user_admin(conn, id_str, data);
}

/**
 * Supprime un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l utilisateur à supprimer
 */
void user_repo_del(PGconn *conn, int id) {
	char id_str[12];
	sprintf(id_str, "%d", id);
	const char *params[1] = {id_str};
	PQclear(PQexecParams(conn, "DELETE FROM users WHERE id = $1", 1, NULL, params, NULL, NULL, 0));
}

/**
 * Parcourt tous les utilisateurs via callback.
 *
 * @param conn Connexion PostgreSQL
 * @param cb Fonction callback appelée pour chaque utilisateur
 * @param ctx Données utilisateur passées au callback
 */
void user_repo_each(PGconn *conn, void (*cb)(User*, void*), void *ctx) {
	PGresult *res = PQexec(conn, "SELECT id, name, email, password_hash, is_admin FROM users");
	if (PQresultStatus(res) != PGRES_TUPLES_OK) { PQclear(res); return; }
	for (int idx = 0; idx < PQntuples(res); idx++) {
		User usr;
		fill_user(&usr, res, idx);
		cb(&usr, ctx);
	}
	PQclear(res);
}
