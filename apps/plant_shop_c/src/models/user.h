#ifndef MODEL_USER_H
#define MODEL_USER_H
/** """ Table users
	@id           clé primaire
	@name         nom complet
	@email        email unique
	@password_hash hash Argon2
	@is_admin     0/1 """ */
typedef struct {
	int  id;
	char name[64];
	char email[64];
	char password_hash[128];
	int  is_admin;
} User;
#endif
