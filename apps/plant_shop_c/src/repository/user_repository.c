#include "user_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/**
 * Remplit une structure User depuis un résultat PostgreSQL.
 *
 * @param u Pointeur vers la structure User à remplir
 * @param r Résultat PostgreSQL
 * @param row Index de la ligne à lire
 */
static void fill_user(User *u, PGresult *r, int row) {
    u->id = atoi(PQgetvalue(r, row, 0));
    strncpy(u->name, PQgetvalue(r, row, 1), sizeof(u->name) - 1);
    u->name[sizeof(u->name) - 1] = '\0';
    strncpy(u->email, PQgetvalue(r, row, 2), sizeof(u->email) - 1);
    u->email[sizeof(u->email) - 1] = '\0';
    strncpy(u->password_hash, PQgetvalue(r, row, 3), sizeof(u->password_hash) - 1);
    u->password_hash[sizeof(u->password_hash) - 1] = '\0';
    u->is_admin = (PQgetvalue(r, row, 4)[0] == 't');
}

/**
 * Ajoute un utilisateur en base de données.
 *
 * @param conn Connexion PostgreSQL
 * @param u Pointeur vers le User à insérer
 * @return ID de l'utilisateur créé, 0 si erreur
 */
int user_repo_add(PGconn *conn, const User *u) {
    const char* params[4] = {u->name, u->email, u->password_hash, u->is_admin ? "t" : "f"};
    PGresult *r = PQexecParams(conn,
        "INSERT INTO users (name, email, password_hash, is_admin) VALUES ($1, $2, $3, $4) RETURNING id",
        4, NULL, params, NULL, NULL, 0);

    if (PQresultStatus(r) != PGRES_TUPLES_OK) {
        // fprintf(stderr, "user_repo_add failed: %s\n", PQerrorMessage(conn));
        PQclear(r);
        return 0;
    }
    int id = atoi(PQgetvalue(r, 0, 0));
    PQclear(r);
    return id;
}

/**
 * Recherche un utilisateur par son ID.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l'utilisateur
 * @param out Pointeur vers la structure à remplir
 * @return 1 si trouvé, 0 sinon
 */
int user_repo_find(PGconn *conn, int id, User *out) {
    char id_str[12];
    sprintf(id_str, "%d", id);
    const char *params[1] = {id_str};
    PGresult *r = PQexecParams(conn,
        "SELECT id, name, email, password_hash, is_admin FROM users WHERE id = $1",
        1, NULL, params, NULL, NULL, 0);

    int found = PQntuples(r);
    if (found) {
        fill_user(out, r, 0);
    } else {
    }
    PQclear(r);
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
    // printf("[DEBUG][SQL] Recherche email : '%s'\n", email);
    const char *params[1] = {email};
    PGresult *r = PQexecParams(conn,
        "SELECT id, name, email, password_hash, is_admin FROM users WHERE email = $1",
        1, NULL, params, NULL, NULL, 0);

    int found = PQntuples(r);
    if (found) {
        fill_user(out, r, 0);
        // printf("[DEBUG][SQL] Utilisateur trouvé : email='%s' hash='%s'\n",
            // PQgetvalue(r, 0, 2), PQgetvalue(r, 0, 3));
    } else {
        // printf("[DEBUG][SQL] Aucun utilisateur trouvé\n");
    }
    PQclear(r);
    return found;
}

/**
 * Vérifie si un utilisateur est administrateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l'utilisateur
 * @return 1 si admin, 0 sinon
 */
int user_repo_is_admin(PGconn *conn, int id) {

    if (id == 0) {

        return 0;
    }
    char id_str[12];
    sprintf(id_str, "%d", id);
    const char *params[1] = {id_str};
    PGresult *r = PQexecParams(conn, "SELECT is_admin FROM users WHERE id = $1", 1, NULL, params, NULL, NULL, 0);

    int is_admin = 0;
    if (PQntuples(r) > 0) {
        is_admin = (PQgetvalue(r, 0, 0)[0] == 't');

    }
    PQclear(r);
    return is_admin;
}

/**
 * Met à jour un utilisateur (name, email, admin).
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l'utilisateur
 * @param patch_data Objet JSON contenant les champs à modifier
 */
void user_repo_patch(PGconn *conn, int id, cJSON *patch_data) {
    char id_str[12];
    sprintf(id_str, "%d", id);

    cJSON *name = cJSON_GetObjectItem(patch_data, "name");
    if (name && cJSON_IsString(name)) {
        const char *params[2] = {name->valuestring, id_str};
        PQclear(PQexecParams(conn, "UPDATE users SET name = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
    }

    cJSON *email = cJSON_GetObjectItem(patch_data, "email");
    if (email && cJSON_IsString(email)) {
        const char *params[2] = {email->valuestring, id_str};
        PQclear(PQexecParams(conn, "UPDATE users SET email = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
    }

    cJSON *admin = cJSON_GetObjectItem(patch_data, "admin");
    if (admin && cJSON_IsBool(admin)) {
        const char *params[2] = {cJSON_IsTrue(admin) ? "t" : "f", id_str};
        PQclear(PQexecParams(conn, "UPDATE users SET is_admin = $1 WHERE id = $2", 2, NULL, params, NULL, NULL, 0));
    }
}

/**
 * Supprime un utilisateur.
 *
 * @param conn Connexion PostgreSQL
 * @param id ID de l'utilisateur à supprimer
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
    PGresult *r = PQexec(conn, "SELECT id, name, email, password_hash, is_admin FROM users");
    if (PQresultStatus(r) != PGRES_TUPLES_OK) {
        // fprintf(stderr, "user_repo_each failed: %s\n", PQerrorMessage(conn));
        PQclear(r);
        return;
    }
    for (int i = 0; i < PQntuples(r); i++) {
        User u;
        fill_user(&u, r, i);
        cb(&u, ctx);
    }
    PQclear(r);
}
