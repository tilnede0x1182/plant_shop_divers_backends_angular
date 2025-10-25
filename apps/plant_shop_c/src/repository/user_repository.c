#ifndef REPO_USER_H
#define REPO_USER_H
#include <libpq-fe.h>
#include "../models/user.h"

/** """ Insère un utilisateur et renvoie l'id
	@conn connexion PG
	@u    pointeur User rempli (name,email,pwd,admin) """ */
int user_repo_insert(PGconn *conn, const User *u);

/** """ Recherche par email
	@conn connexion
	@email email recherché
	@out   User rempli, retourne 1 si trouvé """ */
int user_repo_find_by_email(PGconn *conn,const char *email,User *out);

/** """ Recherche par id
	@conn connexion
	@id   identifiant
	@out  User rempli, 1 si trouvé """ */
int user_repo_find_by_id(PGconn *conn,int id,User *out);

/** """ Met à jour le nom
	@conn connexion
	@id   user_id
	@name nouveau nom """ */
void user_repo_update_name(PGconn *conn,int id,const char *name);

/** """ Met à jour admin flag
	@conn connexion
	@id   user_id
	@flag 0/1 """ */
void user_repo_update_admin(PGconn *conn,int id,int flag);

/** """ Supprime l’utilisateur
	@conn connexion
	@id   user_id """ */
void user_repo_delete(PGconn *conn,int id);

/** """ Liste tous les users
	@conn connexion
	@cb   callback(User*,void*)
	@ctx  contexte utilisateur passé au callback """ */
void user_repo_all(PGconn *conn,void (*cb)(User*,void*),void *ctx);
#endif
