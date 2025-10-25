/** """ Accès SQL table order_items
	@order_item_repo_add   insère un OrderItem
	@order_item_repo_by_order récupère tous les OrderItem d’une commande
	Chaque fonction ≤15 lignes, fichier <100 lignes                                  """ */

#include "order_item_repository.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/** """ Ajoute un item à la commande
	@db  connexion PostgreSQL
	@it  pointeur OrderItem à insérer                                          """ */
void order_item_repo_add(PGconn *db,const OrderItem *it){
	const char *params[4];
	char buf_order[12],buf_plant[12],buf_qty[12],buf_price[12];
	sprintf(buf_order,"%d",it->order_id);
	sprintf(buf_plant,"%d",it->plant_id);
	sprintf(buf_qty  ,"%d",it->qty);
	sprintf(buf_price,"%d",it->price);
	params[0]=buf_order; params[1]=buf_plant; params[2]=buf_qty; params[3]=buf_price;
	PGresult *r=PQexecParams(
		db,
		"INSERT INTO order_items(order_id,plant_id,quantity,price)"
		" VALUES($1,$2,$3,$4)",
		4,NULL,params,NULL,NULL,0);
	if(PQresultStatus(r)!=PGRES_COMMAND_OK){
		fprintf(stderr,"order_item_repo_add: %s\n",PQerrorMessage(db));
	}
	PQclear(r);
}

/** """ Parcourt tous les items d’une commande
	@db   connexion
	@oid  id de la commande
	@cb   callback(OrderItem*,userdata)
	@ud   données utilisateur                                                 """ */
void order_item_repo_by_order(PGconn *db,int oid,
		void(*cb)(OrderItem*,void*),void *ud){
	char buf[12]; sprintf(buf,"%d",oid);
	const char *p[1]={buf};
	PGresult *r=PQexecParams(
		db,
		"SELECT id,order_id,plant_id,quantity,price FROM order_items WHERE order_id=$1",
		1,NULL,p,NULL,NULL,0);
	if(PQresultStatus(r)!=PGRES_TUPLES_OK){
		fprintf(stderr,"order_item_repo_by_order: %s\n",PQerrorMessage(db));
		PQclear(r); return;
	}
	int rows=PQntuples(r);
	for(int i=0;i<rows;i++){
		OrderItem it={
			.id       =atoi(PQgetvalue(r,i,0)),
			.order_id =atoi(PQgetvalue(r,i,1)),
			.plant_id =atoi(PQgetvalue(r,i,2)),
			.qty      =atoi(PQgetvalue(r,i,3)),
			.price    =atoi(PQgetvalue(r,i,4))
		};
		cb(&it,ud);
	}
	PQclear(r);
}
