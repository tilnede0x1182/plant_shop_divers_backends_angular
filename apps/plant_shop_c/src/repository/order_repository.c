#include "order_repository.h"
#include <stdio.h>
#include <string.h>

static void fill(Order *o,PGresult *r){
	o->id=atoi(PQgetvalue(r,0,0));
	o->user_id=atoi(PQgetvalue(r,0,1));
	o->total=atoi(PQgetvalue(r,0,2));
	strcpy(o->status,PQgetvalue(r,0,3));
}

int order_repo_insert(PGconn *c,const Order *o){
	char uid[12],tot[12]; sprintf(uid,"%d",o->user_id); sprintf(tot,"%d",o->total);
	const char *v[3]={uid,tot,o->status};
	PGresult *r=PQexecParams(c,
		"INSERT INTO orders(user_id,total,status) VALUES($1,$2,$3) RETURNING id",
		3,NULL,v,NULL,NULL,0);
	int id=atoi(PQgetvalue(r,0,0)); PQclear(r); return id;
}

int order_repo_find(PGconn *c,int id,Order *o){
	char sid[12]; sprintf(sid,"%d",id); const char *v[1]={sid};
	PGresult *r=PQexecParams(c,"SELECT id,user_id,total,status FROM orders WHERE id=$1",1,NULL,v,NULL,NULL,0);
	if(PQntuples(r)){ fill(o,r); PQclear(r); return 1;} PQclear(r); return 0;
}

void order_repo_update_status(PGconn *c,int id,const char *st){
	char sid[12]; sprintf(sid,"%d",id); const char *v[2]={st,sid};
	PQclear(PQexecParams(c,"UPDATE orders SET status=$1 WHERE id=$2",2,NULL,v,NULL,NULL,0));
}

void order_repo_update_total(PGconn *c,int id,int tot){
	char sid[12],t[12]; sprintf(sid,"%d",id); sprintf(t,"%d",tot);
	const char *v[2]={t,sid};
	PQclear(PQexecParams(c,"UPDATE orders SET total=$1 WHERE id=$2",2,NULL,v,NULL,NULL,0));
}

void order_repo_delete(PGconn *c,int id){
	char sid[12]; sprintf(sid,"%d",id); const char *v[1]={sid};
	PQclear(PQexecParams(c,"DELETE FROM orders WHERE id=$1",1,NULL,v,NULL,NULL,0));
}

void order_repo_all_by_user(PGconn *c,int uid,void(*cb)(Order*,void*),void *ctx){
	char s[12]; sprintf(s,"%d",uid); const char *v[1]={s};
	PGresult *r=PQexecParams(c,"SELECT id,user_id,total,status FROM orders WHERE user_id=$1",1,NULL,v,NULL,NULL,0);
	for(int i=0;i<PQntuples(r);i++){
		Order o;
		o.id=atoi(PQgetvalue(r,i,0));
		o.user_id=atoi(PQgetvalue(r,i,1));
		o.total=atoi(PQgetvalue(r,i,2));
		strcpy(o.status,PQgetvalue(r,i,3));
		cb(&o,ctx);
	}
	PQclear(r);
}
