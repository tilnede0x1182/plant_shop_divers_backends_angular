/** """ Déclaration des routes Kore
	Un groupe de routes/ressource par fonction (≤15 lignes chacune)           """ */
#include <kore/kore.h>
#include "routes.h"
#include "controllers/user_controller.h"
#include "controllers/plant_controller.h"
#include "controllers/order_controller.h"
#include "controllers/auth_controller.h"

/* --- handler basique --- */
int api_ping(struct http_request *req){
	http_response(req,200,NULL,0);
	return KORE_RESULT_OK;
}

/* --- Auth routes (≤15 lignes) --- */
static void auth_routes(void){
	kore_route_add("/api/auth/login","POST",auth_login);
	kore_route_add("/api/auth/register","POST",auth_register);
	kore_route_add("/api/auth/me","GET",auth_me);
}

/* --- Plant routes (≤15 lignes) --- */
static void plant_routes(void){
	kore_route_add("/api/plants/:id","GET",plant_get);
	kore_route_add("/api/admin/plants","GET",admin_plants_list);
	kore_route_add("/api/admin/plants","POST",admin_plants_add);
	kore_route_add("/api/admin/plants/:id","PATCH",admin_plants_patch);
	kore_route_add("/api/admin/plants/:id","DELETE",admin_plants_del);
}

/* --- User routes (≤15 lignes) --- */
static void user_routes(void){
	kore_route_add("/api/users","POST",user_create);
	kore_route_add("/api/users/:id","GET",user_get);
	kore_route_add("/api/users/:id","PATCH",user_patch);
	kore_route_add("/api/users/:id","DELETE",user_del);
	kore_route_add("/api/admin/users","GET",admin_users_list);
}

/* --- Order routes (≤15 lignes) --- */
static void order_routes(void){
	kore_route_add("/api/orders","GET",orders_list);
	kore_route_add("/api/orders","POST",orders_create);
	kore_route_add("/api/orders/:id","PATCH",orders_patch);
	kore_route_add("/api/orders/:id","DELETE",orders_del);
}

/* --- Registre global (≤15 lignes) --- */
void register_routes(void){
	kore_route_add("/api/ping","GET",api_ping);
	auth_routes();
	plant_routes();
	user_routes();
	order_routes();
}
