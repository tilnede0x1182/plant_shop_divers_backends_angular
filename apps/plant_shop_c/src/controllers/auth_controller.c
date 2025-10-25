#include "auth_controller.h"
#include <cjson/cJSON.h>
#include <argon2.h>
#include "../repository/user_repository.h"
extern PGconn* DB;						/* défini dans main.c */

/* --- helpers (≤15 lignes) --- */
static void send_json(struct http_request*req,cJSON*j,int code){
	char* txt=cJSON_PrintUnformatted(j);
	http_response(req,code,txt,strlen(txt));
	free(txt); cJSON_Delete(j);
}

int auth_register(struct http_request*req){
	size_t len; const uint8_t *body=http_body_read(req,&len);
	cJSON* j=cJSON_ParseWithLength((const char*)body,len);
	const char *n=cJSON_GetStringValue(cJSON_GetObjectItem(j,"name"));
	const char *e=cJSON_GetStringValue(cJSON_GetObjectItem(j,"email"));
	const char *p=cJSON_GetStringValue(cJSON_GetObjectItem(j,"password"));
	if(!n||!e||!p){ http_response(req,400,NULL,0); return KORE_RESULT_OK; }
	char hash[128]; argon2_hash(2,1<<16,1,p,strlen(p),hash,sizeof hash,hash,sizeof hash,Argon2_id,19);
	User u={.name="",.email="",.password_hash="",.is_admin=0};
	snprintf(u.name,sizeof u.name,"%s",n);
	snprintf(u.email,sizeof u.email,"%s",e);
	snprintf(u.password_hash,sizeof u.password_hash,"%s",hash);
	u.id=user_repo_add(DB,&u);
	cJSON *out=cJSON_CreateObject(); cJSON_AddNumberToObject(out,"id",u.id);
	send_json(req,out,201); return KORE_RESULT_OK;
}

int auth_login(struct http_request*req){
	size_t len; const uint8_t *body=http_body_read(req,&len);
	cJSON* j=cJSON_ParseWithLength((const char*)body,len);
	const char* e=cJSON_GetStringValue(cJSON_GetObjectItem(j,"email"));
	const char* p=cJSON_GetStringValue(cJSON_GetObjectItem(j,"password"));
	User u; if(!user_repo_find_by_mail(DB,e,&u)){ http_response(req,401,NULL,0); return KORE_RESULT_OK; }
	if(argon2_verify(u.password_hash,p,strlen(p),Argon2_id)!=ARGON2_OK){ http_response(req,401,NULL,0); return KORE_RESULT_OK; }
	/* cookie JWT simplifié = user id */
	char cookie[64]; sprintf(cookie,"jwt=%d; Path=/; HttpOnly",u.id);
	http_response_header(req,"Set-Cookie",cookie);
	cJSON *o=cJSON_CreateObject(); cJSON_AddStringToObject(o,"email",u.email);
	send_json(req,o,201); return KORE_RESULT_OK;
}

int auth_me(struct http_request*req){
	const char *hdr=http_request_header(req,"cookie");
	if(!hdr){ http_response(req,401,NULL,0); return KORE_RESULT_OK; }
	int uid=atoi(strchr(hdr,'=')+1); User u;
	if(!user_repo_find(DB,uid,&u)){ http_response(req,401,NULL,0); return KORE_RESULT_OK; }
	cJSON* o=cJSON_CreateObject(); cJSON_AddStringToObject(o,"email",u.email);
	cJSON_AddStringToObject(o,"name",u.name); cJSON_AddNumberToObject(o,"id",u.id);
	send_json(req,o,200); return KORE_RESULT_OK;
}
