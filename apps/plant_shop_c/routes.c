#include <stdio.h>
#include <string.h>
#include "mongoose/mongoose.h"
#include "routes.h"
#include "src/controllers/auth_controller.h"
#include "src/controllers/plant_controller.h"
#include "src/controllers/user_controller.h"
#include "src/controllers/order_controller.h"
#include "src/controllers/order_item_controller.h"
#include "src/utils/cors.h"

/* -------- trace utilitaire -------- */
static void log_route(struct mg_http_message *hm, const char *h) {
	fprintf(stderr, "[ROUTE] %.*s %.*s → %s\n",
	        (int) hm->method.len, hm->method.buf,
	        (int) hm->uri.len,    hm->uri.buf,
	        h);
}

/* -------- /api/ping -------- */
static void api_ping(struct mg_connection *c, struct mg_http_message *hm) {
	log_route(hm, "api_ping");
	cors_reply(c, 200, "", "");
}

/* -------- routeur principal -------- */
void route_request(struct mg_connection *c, struct mg_http_message *hm) {
  if (cors_handle_preflight(c, hm)) return;
	int id = 0;                               /* buffer id */

	/* ----- Auth ----- */
	if (mg_http_match_uri(hm, "/api/auth/login")) {
		log_route(hm, "auth_login"),  auth_login(c, hm);
	} else if (mg_http_match_uri(hm, "/api/auth/register")) {
		log_route(hm, "auth_register"), auth_register(c, hm);
	} else if (mg_http_match_uri(hm, "/api/auth/me")) {
		log_route(hm, "auth_me"), auth_me(c, hm);
	} else if (mg_http_match_uri(hm, "/api/auth/logout")) {
		if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
			log_route(hm, "auth_logout"), auth_logout(c, hm);
		} else {
			cors_reply(c, 405, "Allow: POST\r\n", "");
		}
	}

	/* ----- Plants ----- */
	else if (mg_http_match_uri(hm, "/api/plants")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "plants_list_public"), plants_list_public(c, hm);
		} else {
			cors_reply(c, 405, "Allow: GET\r\n", "");
		}
	} else if (sscanf(hm->uri.buf, "/api/plants/%d", &id) == 1) {
		log_route(hm, "plant_get"), plant_get(c, hm, id);
	} else if (mg_http_match_uri(hm, "/api/admin/plants")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "admin_plants_list"), admin_plants_list(c, hm);
		} else if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
			log_route(hm, "admin_plants_add"),  admin_plants_add(c, hm);
		}
	} else if (sscanf(hm->uri.buf, "/api/admin/plants/%d", &id) == 1) {
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "admin_plants_patch"), admin_plants_patch(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "admin_plants_del"),  admin_plants_del(c, hm, id);
		}
	}

	/* ----- Users ----- */
	else if (mg_http_match_uri(hm, "/api/users")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "admin_users_list");
			admin_users_list(c, hm);        /* gestion GET /api/users */
		} else if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
			log_route(hm, "user_create");
			user_create(c, hm);
		}
	} else if (sscanf(hm->uri.buf, "/api/users/%d", &id) == 1) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "user_get");
			user_get(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "user_patch");
			user_patch(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "user_del");
			user_del(c, hm, id);
		}
	}

	/* ----- Admin users ----- */
	else if (mg_http_match_uri(hm, "/api/admin/users")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "admin_users_list");
			admin_users_list(c, hm);
		}
	}

	/* ----- Alias admin sur /api/admin/users/:id ----- */
	else if (sscanf(hm->uri.buf, "/api/admin/users/%d", &id) == 1) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "admin_user_get");
			user_get(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "admin_user_patch");
			user_patch(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "admin_user_del");
			user_del(c, hm, id);
		} else {
			cors_reply(c, 405, "Allow: GET,PATCH,DELETE\r\n", "");
		}
	}

	/* ----- Orders (items avant id, id avant liste) ----- */
	else if (mg_http_match_uri(hm, "/api/orders/*/items")) {    /* /orders/:id/items */
		if (sscanf(hm->uri.buf, "/api/orders/%d/items", &id) == 1) {
			log_route(hm, "order_items_by_order"), order_items_by_order(c, hm, id);
		}
	} else if (sscanf(hm->uri.buf, "/api/orders/%d", &id) == 1) { /* /orders/:id */
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "orders_patch"), orders_patch(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "orders_del"), orders_del(c, hm, id);
		}
	} else if (mg_http_match_uri(hm, "/api/orders")) {          /* /orders */
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "orders_list"), orders_list(c, hm);
		} else if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
			log_route(hm, "orders_create"), orders_create(c, hm);
		}
	}

	/* ----- Order-items individuels ----- */
	else if (sscanf(hm->uri.buf, "/api/order-items/%d", &id) == 1) {
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "order_item_patch"), order_item_patch(c, hm, id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "order_item_del"),  order_item_del(c, hm, id);
		}
	}

	/* ----- Ping et 404 ----- */
	else if (mg_http_match_uri(hm, "/api/ping")) {
		api_ping(c, hm);
	} else {
		log_route(hm, "404"), cors_reply(c, 404, "Content-Type: text/plain\r\n", "Not Found\n");
	}
}
