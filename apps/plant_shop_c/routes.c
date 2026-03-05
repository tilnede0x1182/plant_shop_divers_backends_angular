/* ==============================================================================
   Importations
   ============================================================================== */
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

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/* ------------------------------------------------------------------------------
   Logging et debug
   ------------------------------------------------------------------------------ */
/**
 * Trace une route pour le debug (desactive).
 *
 * @param hm Message HTTP recu
 * @param handler_name Nom du handler
 */
static void log_route(struct mg_http_message *hm, const char *handler_name) {
	// fprintf(stderr, "[ROUTE] %.*s %.*s -> %s\n",
	//         (int) hm->method.len, hm->method.buf,
	//         (int) hm->uri.len,    hm->uri.buf,
	//         h);
}

/* ------------------------------------------------------------------------------
   Handlers simples
   ------------------------------------------------------------------------------ */
/**
 * Handler pour le endpoint de sante /api/ping.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
static void api_ping(struct mg_connection *c, struct mg_http_message *hm) {
	log_route(hm, "api_ping");
	cors_reply(c, 200, "", "");
}

/* ------------------------------------------------------------------------------
   Routeurs par domaine
   ------------------------------------------------------------------------------ */
/**
 * Route les requetes d authentification.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @return 1 si route trouvee, 0 sinon
 */
static int route_auth(struct mg_connection *c, struct mg_http_message *hm) {
	if (mg_http_match_uri(hm, "/api/auth/login")) { auth_login(c, hm); return 1; }
	if (mg_http_match_uri(hm, "/api/auth/register")) { auth_register(c, hm); return 1; }
	if (mg_http_match_uri(hm, "/api/auth/me")) { auth_me(c, hm); return 1; }
	if (mg_http_match_uri(hm, "/api/auth/logout")) {
		if (mg_strcmp(hm->method, mg_str("POST")) == 0) auth_logout(c, hm);
		else cors_reply(c, 405, "Allow: POST\r\n", "");
		return 1;
	}
	return 0;
}

/**
 * Route les requetes publiques sur les plantes.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_plants_public(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (mg_http_match_uri(hm, "/api/plants")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
			log_route(hm, "plants_list_public"), plants_list_public(c, hm);
		} else {
			cors_reply(c, 405, "Allow: GET\r\n", "");
		}
		return 1;
	}
	if (sscanf(hm->uri.buf, "/api/plants/%d", id) == 1) {
		log_route(hm, "plant_get"), plant_get(c, hm, *id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes admin sur les plantes.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_plants_admin(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (mg_http_match_uri(hm, "/api/admin/plants")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) admin_plants_list(c, hm);
		else if (mg_strcmp(hm->method, mg_str("POST")) == 0) admin_plants_add(c, hm);
		return 1;
	}
	if (sscanf(hm->uri.buf, "/api/admin/plants/%d", id) == 1) {
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) admin_plants_patch(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) admin_plants_del(c, hm, *id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur /api/users.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_users(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (mg_http_match_uri(hm, "/api/users")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) admin_users_list(c, hm);
		else if (mg_strcmp(hm->method, mg_str("POST")) == 0) user_create(c, hm);
		return 1;
	}
	if (sscanf(hm->uri.buf, "/api/users/%d", id) == 1) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) user_get(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) user_patch(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) user_del(c, hm, *id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes admin sur /api/admin/users.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_users_admin(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (mg_http_match_uri(hm, "/api/admin/users")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) admin_users_list(c, hm);
		return 1;
	}
	if (sscanf(hm->uri.buf, "/api/admin/users/%d", id) == 1) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) user_get(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) user_patch(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) user_del(c, hm, *id);
		else cors_reply(c, 405, "Allow: GET,PATCH,DELETE\r\n", "");
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur les commandes.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_orders(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (mg_http_match_uri(hm, "/api/orders/*/items")) {
		if (sscanf(hm->uri.buf, "/api/orders/%d/items", id) == 1) order_items_by_order(c, hm, *id);
		return 1;
	}
	if (sscanf(hm->uri.buf, "/api/orders/%d", id) == 1) {
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) orders_patch(c, hm, *id);
		else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) orders_del(c, hm, *id);
		return 1;
	}
	if (mg_http_match_uri(hm, "/api/orders")) {
		if (mg_strcmp(hm->method, mg_str("GET")) == 0) orders_list(c, hm);
		else if (mg_strcmp(hm->method, mg_str("POST")) == 0) orders_create(c, hm);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur les articles de commande.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 * @param id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_order_items(struct mg_connection *c, struct mg_http_message *hm, int *id) {
	if (sscanf(hm->uri.buf, "/api/order-items/%d", id) == 1) {
		if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
			log_route(hm, "order_item_patch"), order_item_patch(c, hm, *id);
		} else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
			log_route(hm, "order_item_del"), order_item_del(c, hm, *id);
		}
		return 1;
	}
	return 0;
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Routeur principal de l application.
 * Dispatch les requetes vers les controllers appropries.
 *
 * @param c Connexion Mongoose
 * @param hm Message HTTP recu
 */
void route_request(struct mg_connection *c, struct mg_http_message *hm) {
	if (cors_handle_preflight(c, hm)) return;
	int id = 0;
	if (route_auth(c, hm)) return;
	if (route_plants_public(c, hm, &id)) return;
	if (route_plants_admin(c, hm, &id)) return;
	if (route_users(c, hm, &id)) return;
	if (route_users_admin(c, hm, &id)) return;
	if (route_orders(c, hm, &id)) return;
	if (route_order_items(c, hm, &id)) return;
	if (mg_http_match_uri(hm, "/api/ping")) { api_ping(c, hm); return; }
	cors_reply(c, 404, "Content-Type: text/plain\r\n", "Not Found\n");
}
