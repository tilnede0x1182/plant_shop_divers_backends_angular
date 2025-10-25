#include <kore/kore.h>
#include <kore/http.h>
#include "routes.h"
#include "src/controllers/user_controller.h"
#include "src/controllers/plant_controller.h"
#include "src/controllers/order_controller.h"
#include "src/controllers/auth_controller.h"
#include "src/controllers/order_item_controller.h"

/* --- handler basique --- */
int api_ping(struct http_request *req){
	http_response(req,200,NULL,0);
	return KORE_RESULT_OK;
}

/* --- Auth routes --- */
static void auth_routes(void){
	http_route_add("/api/auth/login","POST",auth_login, 0);
	http_route_add("/api/auth/register","POST",auth_register, 0);
	http_route_add("/api/auth/me","GET",auth_me, 0);
}

/* --- Plant routes --- */
static void plant_routes(void){
	http_route_add("/api/plants/:id","GET",plant_get, 0);
	http_route_add("/api/admin/plants","GET",admin_plants_list, 0);
	http_route_add("/api/admin/plants","POST",admin_plants_add, 0);
	http_route_add("/api/admin/plants/:id","PATCH",admin_plants_patch, 0);
	http_route_add("/api/admin/plants/:id","DELETE",admin_plants_del, 0);
}

/* --- User routes --- */
static void user_routes(void){
	http_route_add("/api/users","POST",user_create, 0);
	http_route_add("/api/users/:id","GET",user_get, 0);
	http_route_add("/api/users/:id","PATCH",user_patch, 0);
	http_route_add("/api/users/:id","DELETE",user_del, 0);
	http_route_add("/api/admin/users","GET",admin_users_list, 0);
}

/* --- Order routes --- */
static void order_routes(void){
	http_route_add("/api/orders","GET",orders_list, 0);
	http_route_add("/api/orders","POST",orders_create, 0);
	http_route_add("/api/orders/:id","PATCH",orders_patch, 0);
	http_route_add("/api/orders/:id","DELETE",orders_del, 0);
    http_route_add("/api/orders/:id/items", "GET", order_items_by_order, 0);
}

/* --- Order-item routes --- */
static void order_item_routes(void) {
	http_route_add("/api/order-items/:id", "PATCH", order_item_patch, 0);
	http_route_add("/api/order-items/:id", "DELETE", order_item_del, 0);
}

/* --- Registre global --- */
void register_routes(void){
	http_route_add("/api/ping","GET",api_ping, 0);
	auth_routes();
	plant_routes();
	user_routes();
	order_routes();
    order_item_routes();
}
