/* ---------- Importations ---------- */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <curl/curl.h>
#include <cjson/cJSON.h>

/* ---------- Constantes ---------- */
#define BASE "http://localhost:4100/api"
#define ADMIN_MAIL "admin1@planteshop.com"
#define ADMIN_PWD  "password"

/* ---------- Buffer HTTP ---------- */
struct Buf { char *data; size_t len; };
static size_t wr(void *ptr,size_t s,size_t n,void *u){
	struct Buf *b=u; size_t L=s*n;
	b->data=realloc(b->data,b->len+L+1);
	memcpy(b->data+b->len,ptr,L); b->len+=L; b->data[b->len]=0; return L;
}

/* ---------- Appel HTTP JSON ---------- */
static cJSON* call(const char* m,const char* path,int exp,const char* json,
                   struct curl_slist** ck){
	CURL* c=curl_easy_init(); struct Buf buf={0};
	char url[128]; sprintf(url,"%s%s",BASE,path);
	curl_easy_setopt(c,CURLOPT_URL,url);
	if(strcmp(m,"GET")) curl_easy_setopt(c,CURLOPT_CUSTOMREQUEST,m);
	if(json) curl_easy_setopt(c,CURLOPT_POSTFIELDS,json);
	curl_easy_setopt(c,CURLOPT_WRITEFUNCTION,wr);
	curl_easy_setopt(c,CURLOPT_WRITEDATA,&buf);
	curl_easy_setopt(c,CURLOPT_HTTPHEADER,
	    curl_slist_append(NULL,"Content-Type: application/json"));
	if(*ck) curl_easy_setopt(c,CURLOPT_HTTPHEADER,*ck);
	curl_easy_setopt(c,CURLOPT_HEADERFUNCTION,wr);
	curl_easy_setopt(c,CURLOPT_HEADERDATA,&buf);
	CURLcode res=curl_easy_perform(c);
	long code; curl_easy_getinfo(c,CURLINFO_RESPONSE_CODE,&code);
	printf("%s %-6s %s [%ld]\n",code==exp?"✅":"❌",m,path,code);
	if(code!=exp||res) exit(1);
	/* cookies */
	char* hdr; size_t idx=0;
	while(!curl_easy_getinfo(c,CURLINFO_COOKIELIST,&hdr)&&hdr){
		if(!*ck) *ck=NULL;
		char* semi=strchr(hdr,';'); if(semi) *semi='\0';
		char h[128]; sprintf(h,"Cookie: %s",hdr);
		*ck=curl_slist_append(*ck,h);
		curl_slist_free_all((struct curl_slist*)hdr);
		curl_easy_getinfo(c,CURLINFO_COOKIELIST,&hdr);
	}
	curl_easy_cleanup(c);
	if(!buf.len) return NULL;
	return cJSON_Parse(buf.data);
}

/* ---------- Helpers ---------- */
static void assert_eq(cJSON* o,const char* k,const char* v){
	cJSON* n=cJSON_GetObjectItem(o,k);
	if(!n||strcmp(n->valuestring,v)) exit(2);
}

/* ---------- Suites de tests ---------- */
static void test_plants(struct curl_slist* admin){
	cJSON *body=cJSON_CreateObject();
	cJSON_AddStringToObject(body,"name","Test Plant");
	cJSON_AddNumberToObject(body,"price",10);
	cJSON_AddNumberToObject(body,"stock",5);
	cJSON* r=call("POST","/admin/plants",201,cJSON_PrintUnformatted(body),&admin);
	int id=cJSON_GetObjectItem(r,"id")->valueint;
	char path[64]; sprintf(path,"/plants/%d",id);
	cJSON* g=call("GET",path,200,NULL,&admin);
	assert_eq(g,"name","Test Plant");
	cJSON_Delete(r); cJSON_Delete(g);
}

/* ---------- Main ---------- */
int main(void){
	srand((unsigned)time(NULL)); curl_global_init(CURL_GLOBAL_DEFAULT);
	struct curl_slist *admin=NULL,*user=NULL;

	/* login admin */
	cJSON *j=cJSON_CreateObject();
	cJSON_AddStringToObject(j,"email",ADMIN_MAIL);
	cJSON_AddStringToObject(j,"password",ADMIN_PWD);
	call("POST","/auth/login",201,cJSON_PrintUnformatted(j),&admin);

	/* register+login user */
	char mail[64]; sprintf(mail,"u_%ld@example.com",time(NULL));
	cJSON *reg=cJSON_CreateObject();
	cJSON_AddStringToObject(reg,"name","User");
	cJSON_AddStringToObject(reg,"email",mail);
	cJSON_AddStringToObject(reg,"password","pass123");
	call("POST","/auth/register",201,cJSON_PrintUnformatted(reg),&user);
	call("POST","/auth/login",201,cJSON_PrintUnformatted(reg),&user);

	/* tests */
	test_plants(admin);

	puts("🎉 Tests C terminés");
	curl_global_cleanup(); return 0;
}
