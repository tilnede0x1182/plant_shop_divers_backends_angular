#include "user_controller.h"
#include <cjson/cJSON.h>
#include "../repository/user_repository.h"
extern PGconn* DB;

/* util sortie json courte */
static void jout(struct http_request*r,cJSON*j,int c){
	char *t=cJSON_PrintUnformatted(j); http_response(r,c,t,strlen(t)); free(t); cJSON_Delete(j);
}
static int is_admin(struct http_request*req){
	const char*ck=http_request_header(req,"cookie"); if(!ck) return 0;
	return user_repo_is_admin(DB,atoi(strchr(ck,'=')+1));
}

int user_create(struct http_request*req){
	if(!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	size_t len; const uint8_t* b=http_body_read(req,&len);
	cJSON* j=cJSON_ParseWithLength((const char*)b,len);
	User u={0}; snprintf(u.name,sizeof u.name,"%s",cJSON_GetStringValue(cJSON_GetObjectItem(j,"name")));
	snprintf(u.email,sizeof u.email,"%s",cJSON_GetStringValue(cJSON_GetObjectItem(j,"email")));
	snprintf(u.password_hash,sizeof u.password_hash,"x"); u.is_admin=cJSON_IsTrue(cJSON_GetObjectItem(j,"admin"));
	u.id=user_repo_add(DB,&u); cJSON *o=cJSON_CreateObject(); cJSON_AddNumberToObject(o,"id",u.id);
	jout(req,o,201); return KORE_RESULT_OK;
}

int user_get(struct http_request*req){
	int id=http_populate_get_int(req,"id"); User u;
	if(!user_repo_find(DB,id,&u)){ http_response(req,404,NULL,0); return KORE_RESULT_OK; }
	cJSON* j=cJSON_CreateObject(); cJSON_AddStringToObject(j,"name",u.name);
	cJSON_AddStringToObject(j,"email",u.email); cJSON_AddNumberToObject(j,"id",u.id);
	cJSON_AddBoolToObject(j,"admin",u.is_admin);
	jout(req,j,200); return KORE_RESULT_OK;
}

int user_patch(struct http_request*req){
	int id=http_populate_get_int(req,"id"); const char* ck=http_request_header(req,"cookie");
	int uid=ck?atoi(strchr(ck,'=')+1):0; if(uid!=id&&!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	size_t len; const uint8_t* b=http_body_read(req,&len);
	cJSON* j=cJSON_ParseWithLength((const char*)b,len);
	user_repo_patch(DB,id,j); http_response(req,200,NULL,0); return KORE_RESULT_OK;
}

int user_del(struct http_request*req){
	if(!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	int id=http_populate_get_int(req,"id"); user_repo_del(DB,id); http_response(req,200,NULL,0);
	return KORE_RESULT_OK;
}

int admin_users_list(struct http_request*req){
	if(!is_admin(req)){ http_response(req,403,NULL,0); return KORE_RESULT_OK; }
	cJSON* arr=cJSON_CreateArray();
	user_repo_each(DB,[](User*u,void*a){
		cJSON* j=cJSON_CreateObject(); cJSON_AddNumberToObject(j,"id",u->id);
		cJSON_AddStringToObject(j,"email",u->email); cJSON_AddStringToObject(j,"name",u->name);
		cJSON_AddItemToArray((cJSON*)a,j);
	},arr);
	jout(req,arr,200); return KORE_RESULT_OK;
}
