/** """ Contrôleur order_items
		- order_items_by_order  : user ou admin, liste les items d’une commande
		- order_item_patch/del : admin uniquement
		Utilise repository, reste <100 lignes                                  """ */

#include "order_item_controller.h"
#include <cjson/cJSON.h>
#include "../repository/order_item_repository.h"
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include "../utils/utils.h"
extern PGconn* DB;

/* ---------- helpers droits ---------- */
static int current_uid(struct http_request*req){
	const char *ck=http_request_header(req,"cookie");
	return ck ? atoi(strchr(ck,'=')+1) : 0;
}
static int is_admin(struct http_request*req){
	return user_repo_is_admin(DB,current_uid(req));
}

/* ---------- sortie JSON courte ---------- */
static void json_out(struct http_request*r,cJSON*j,int code){
	char *txt=cJSON_PrintUnformatted(j);
	http_response(r,code,txt,strlen(txt));
	free(txt); cJSON_Delete(j);
}

/* ---------- GET /orders/:id/items ---------- */
int order_items_by_order(struct http_request*req){
	int oid=http_populate_get_int(req,"id");      /* id de la commande */
	int uid=current_uid(req);
	if(!uid){ http_response(req,401,NULL,0); return KORE_RESULT_OK; }
	if(!is_admin(req) && !order_repo_belongs_to(DB,oid,uid)){
		http_response(req,403,NULL,0); return KORE_RESULT_OK;
	}
	cJSON *arr=cJSON_CreateArray();
	order_item_repo_by_order(DB,oid,[](OrderItem *it,void *ud){
		cJSON *j=cJSON_CreateObject();
		cJSON_AddNumberToObject(j,"id",it->id);
		cJSON_AddNumberToObject(j,"plantId",it->plant_id);
		cJSON_AddNumberToObject(j,"quantity",it->qty);
		cJSON_AddNumberToObject(j,"price",it->price);
		cJSON_AddItemToArray((cJSON*)ud,j);
	},arr);
	json_out(req,arr,200); return KORE_RESULT_OK;
}

/* ---------- PATCH /order-items/:id ---------- */
int order_item_patch(struct http_request*req){
	if(!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	int id=http_populate_get_int(req,"id");
	size_t len; const uint8_t *b=http_body_read(req,&len);
	cJSON *upd=cJSON_ParseWithLength((const char*)b,len);
	order_item_repo_patch(DB,id,upd);   /* implémenter côté repo si besoin */
	http_response(req,200,NULL,0); cJSON_Delete(upd);
	return KORE_RESULT_OK;
}

/* ---------- DELETE /order-items/:id ---------- */
int order_item_del(struct http_request*req){
	if(!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	int id=http_populate_get_int(req,"id");
	order_item_repo_del(DB,id);         /* implémenter côté repo si besoin */
	http_response(req,200,NULL,0);
	return KORE_RESULT_OK;
}
