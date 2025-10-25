#include "user_repository.h"
#include <stdio.h>
#include <string.h>

/* ---------- Helpers internes ---------- */
static void fill_user(User *u,PGresult *r){
	u->id         = atoi(PQgetvalue(r,0,0));
	strcpy(u->name,PQgetvalue(r,0,1));
	strcpy(u->email,PQgetvalue(r,0,2));
	strcpy(u->password_hash,PQgetvalue(r,0,3));
	u->is_admin   = !strcmp(PQgetvalue(r,0,4),"t");
}

int user_repo_insert(PGconn *c,const User *u){
	const char *p[4]={u->name,u->email,u->password_hash,u->is_admin?"t":"f"};
	PGresult *r=PQexecParams(c,
		"INSERT INTO users(name,email,password_hash,is_admin) VALUES($1,$2,$3,$4) RETURNING id",
		4,NULL,p,NULL,NULL,0);
	int id=atoi(PQgetvalue(r,0,0)); PQclear(r); return id;
}

int user_repo_find_by_email(PGconn *c,const char *e,User *out){
	const char *p[1]={e};
	PGresult *r=PQexecParams(c,"SELECT id,name,email,password_hash,is_admin FROM users WHERE email=$1",1,NULL,p,NULL,NULL,0);
	if(PQntuples(r)){ fill_user(out,r); PQclear(r); return 1; } PQclear(r); return 0;
}

int user_repo_find_by_id(PGconn *c,int id,User *out){
	char sid[12]; sprintf(sid,"%d",id); const char *p[1]={sid};
	PGresult *r=PQexecParams(c,"SELECT id,name,email,password_hash,is_admin FROM users WHERE id=$1",1,NULL,p,NULL,NULL,0);
	if(PQntuples(r)){ fill_user(out,r); PQclear(r); return 1; } PQclear(r); return 0;
}

void user_repo_update_name(PGconn *c,int id,const char *name){
	char sid[12]; sprintf(sid,"%d",id);
	const char *p[2]={name,sid};
	PQclear(PQexecParams(c,"UPDATE users SET name=$1 WHERE id=$2",2,NULL,p,NULL,NULL,0));
}

void user_repo_update_admin(PGconn *c,int id,int flag){
	char sid[12]; sprintf(sid,"%d",id); const char *p[2]={flag?"t":"f",sid};
	PQclear(PQexecParams(c,"UPDATE users SET is_admin=$1 WHERE id=$2",2,NULL,p,NULL,NULL,0));
}

void user_repo_delete(PGconn *c,int id){
	char sid[12]; sprintf(sid,"%d",id); const char *p[1]={sid};
	PQclear(PQexecParams(c,"DELETE FROM users WHERE id=$1",1,NULL,p,NULL,NULL,0));
}

void user_repo_all(PGconn *c,void (*cb)(User*,void*),void *ctx){
	PGresult *r=PQexec(c,"SELECT id,name,email,password_hash,is_admin FROM users");
	for(int i=0;i<PQntuples(r);i++){
		User u;
		u.id=atoi(PQgetvalue(r,i,0));
		strcpy(u.name,PQgetvalue(r,i,1));
		strcpy(u.email,PQgetvalue(r,i,2));
		strcpy(u.password_hash,PQgetvalue(r,i,3));
		u.is_admin=!strcmp(PQgetvalue(r,i,4),"t");
		cb(&u,ctx);
	}
	PQclear(r);
}
