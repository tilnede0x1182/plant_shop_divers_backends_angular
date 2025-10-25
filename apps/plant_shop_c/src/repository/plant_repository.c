#include "plant_repository.h"
#include <stdio.h>
#include <string.h>

static void fill(Plant *p,PGresult *r){
	p->id=atoi(PQgetvalue(r,0,0));
	strcpy(p->name,PQgetvalue(r,0,1));
	strcpy(p->description,PQgetvalue(r,0,2));
	p->price=atoi(PQgetvalue(r,0,3));
	p->stock=atoi(PQgetvalue(r,0,4));
}

int plant_repo_insert(PGconn *c,const Plant *p){
	char pr[8],st[4]; sprintf(pr,"%d",p->price); sprintf(st,"%d",p->stock);
	const char *v[4]={p->name,p->description,pr,st};
	PGresult *r=PQexecParams(c,
		"INSERT INTO plants(name,description,price,stock) VALUES($1,$2,$3,$4) RETURNING id",
		4,NULL,v,NULL,NULL,0);
	int id=atoi(PQgetvalue(r,0,0)); PQclear(r); return id;
}

int plant_repo_find(PGconn *c,int id,Plant *p){
	char sid[12]; sprintf(sid,"%d",id); const char *v[1]={sid};
	PGresult *r=PQexecParams(c,"SELECT id,name,description,price,stock FROM plants WHERE id=$1",1,NULL,v,NULL,NULL,0);
	if(PQntuples(r)){ fill(p,r); PQclear(r); return 1;} PQclear(r); return 0;
}

void plant_repo_update_price(PGconn *c,int id,int price){
	char sid[12],pr[8]; sprintf(sid,"%d",id); sprintf(pr,"%d",price);
	const char *v[2]={pr,sid};
	PQclear(PQexecParams(c,"UPDATE plants SET price=$1 WHERE id=$2",2,NULL,v,NULL,NULL,0));
}

void plant_repo_update_stock(PGconn *c,int id,int delta){
	char sid[12],d[4]; sprintf(sid,"%d",id); sprintf(d,"%d",delta);
	const char *v[2]={d,sid};
	PQclear(PQexecParams(c,"UPDATE plants SET stock = stock - $1 WHERE id=$2",2,NULL,v,NULL,NULL,0));
}

void plant_repo_delete(PGconn *c,int id){
	char sid[12]; sprintf(sid,"%d",id); const char *v[1]={sid};
	PQclear(PQexecParams(c,"DELETE FROM plants WHERE id=$1",1,NULL,v,NULL,NULL,0));
}

void plant_repo_all(PGconn *c,void(*cb)(Plant*,void*),void *ctx){
	PGresult *r=PQexec(c,"SELECT id,name,description,price,stock FROM plants");
	for(int i=0;i<PQntuples(r);i++){
		Plant p;
		p.id=atoi(PQgetvalue(r,i,0));
		strcpy(p.name,PQgetvalue(r,i,1));
		strcpy(p.description,PQgetvalue(r,i,2));
		p.price=atoi(PQgetvalue(r,i,3));
		p.stock=atoi(PQgetvalue(r,i,4));
		cb(&p,ctx);
	}
	PQclear(r);
}
