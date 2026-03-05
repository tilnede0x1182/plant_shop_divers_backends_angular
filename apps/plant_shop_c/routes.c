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
 * @param http_message Message HTTP recu
 * @param handler_name Nom du handler
 */
static void log_route(struct mg_http_message *http_message, const char *handler_name) {
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
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
static void api_ping(struct mg_connection *mongoose_connection, struct mg_http_message *http_message) {
	log_route(http_message, "api_ping");
	cors_reply(mongoose_connection, 200, "", "");
}

/* ------------------------------------------------------------------------------
   Routeurs par domaine
   ------------------------------------------------------------------------------ */
/**
 * Route les requetes d authentification.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @return 1 si route trouvee, 0 sinon
 */
static int route_auth(struct mg_connection *mongoose_connection, struct mg_http_message *http_message) {
	if (mg_http_match_uri(http_message, "/api/auth/login")) { auth_login(mongoose_connection, http_message); return 1; }
	if (mg_http_match_uri(http_message, "/api/auth/register")) { auth_register(mongoose_connection, http_message); return 1; }
	if (mg_http_match_uri(http_message, "/api/auth/me")) { auth_me(mongoose_connection, http_message); return 1; }
	if (mg_http_match_uri(http_message, "/api/auth/logout")) {
		if (mg_strcmp(http_message->method, mg_str("POST")) == 0) auth_logout(mongoose_connection, http_message);
		else cors_reply(mongoose_connection, 405, "Allow: POST\r\n", "");
		return 1;
	}
	return 0;
}

/**
 * Route les requetes publiques sur les plantes.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_plants_public(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (mg_http_match_uri(http_message, "/api/plants")) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) {
			log_route(http_message, "plants_list_public"), plants_list_public(mongoose_connection, http_message);
		} else {
			cors_reply(mongoose_connection, 405, "Allow: GET\r\n", "");
		}
		return 1;
	}
	if (sscanf(http_message->uri.buf, "/api/plants/%d", extracted_id) == 1) {
		log_route(http_message, "plant_get"), plant_get(mongoose_connection, http_message, *extracted_id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes admin sur les plantes.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_plants_admin(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (mg_http_match_uri(http_message, "/api/admin/plants")) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) admin_plants_list(mongoose_connection, http_message);
		else if (mg_strcmp(http_message->method, mg_str("POST")) == 0) admin_plants_add(mongoose_connection, http_message);
		return 1;
	}
	if (sscanf(http_message->uri.buf, "/api/admin/plants/%d", extracted_id) == 1) {
		if (mg_strcmp(http_message->method, mg_str("PATCH")) == 0) admin_plants_patch(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("DELETE")) == 0) admin_plants_del(mongoose_connection, http_message, *extracted_id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur /api/users.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_users(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (mg_http_match_uri(http_message, "/api/users")) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) admin_users_list(mongoose_connection, http_message);
		else if (mg_strcmp(http_message->method, mg_str("POST")) == 0) user_create(mongoose_connection, http_message);
		return 1;
	}
	if (sscanf(http_message->uri.buf, "/api/users/%d", extracted_id) == 1) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) user_get(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("PATCH")) == 0) user_patch(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("DELETE")) == 0) user_del(mongoose_connection, http_message, *extracted_id);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes admin sur /api/admin/users.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_users_admin(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (mg_http_match_uri(http_message, "/api/admin/users")) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) admin_users_list(mongoose_connection, http_message);
		return 1;
	}
	if (sscanf(http_message->uri.buf, "/api/admin/users/%d", extracted_id) == 1) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) user_get(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("PATCH")) == 0) user_patch(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("DELETE")) == 0) user_del(mongoose_connection, http_message, *extracted_id);
		else cors_reply(mongoose_connection, 405, "Allow: GET,PATCH,DELETE\r\n", "");
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur les commandes.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_orders(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (mg_http_match_uri(http_message, "/api/orders/*/items")) {
		if (sscanf(http_message->uri.buf, "/api/orders/%d/items", extracted_id) == 1) order_items_by_order(mongoose_connection, http_message, *extracted_id);
		return 1;
	}
	if (sscanf(http_message->uri.buf, "/api/orders/%d", extracted_id) == 1) {
		if (mg_strcmp(http_message->method, mg_str("PATCH")) == 0) orders_patch(mongoose_connection, http_message, *extracted_id);
		else if (mg_strcmp(http_message->method, mg_str("DELETE")) == 0) orders_del(mongoose_connection, http_message, *extracted_id);
		return 1;
	}
	if (mg_http_match_uri(http_message, "/api/orders")) {
		if (mg_strcmp(http_message->method, mg_str("GET")) == 0) orders_list(mongoose_connection, http_message);
		else if (mg_strcmp(http_message->method, mg_str("POST")) == 0) orders_create(mongoose_connection, http_message);
		return 1;
	}
	return 0;
}

/**
 * Route les requetes sur les articles de commande.
 *
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 * @param extracted_id Buffer pour ID extrait
 * @return 1 si route trouvee, 0 sinon
 */
static int route_order_items(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int *extracted_id) {
	if (sscanf(http_message->uri.buf, "/api/order-items/%d", extracted_id) == 1) {
		if (mg_strcmp(http_message->method, mg_str("PATCH")) == 0) {
			log_route(http_message, "order_item_patch"), order_item_patch(mongoose_connection, http_message, *extracted_id);
		} else if (mg_strcmp(http_message->method, mg_str("DELETE")) == 0) {
			log_route(http_message, "order_item_del"), order_item_del(mongoose_connection, http_message, *extracted_id);
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
 * @param mongoose_connection Connexion Mongoose
 * @param http_message Message HTTP recu
 */
void route_request(struct mg_connection *mongoose_connection, struct mg_http_message *http_message) {
	if (cors_handle_preflight(mongoose_connection, http_message)) return;
	int extracted_id = 0;
	if (route_auth(mongoose_connection, http_message)) return;
	if (route_plants_public(mongoose_connection, http_message, &extracted_id)) return;
	if (route_plants_admin(mongoose_connection, http_message, &extracted_id)) return;
	if (route_users(mongoose_connection, http_message, &extracted_id)) return;
	if (route_users_admin(mongoose_connection, http_message, &extracted_id)) return;
	if (route_orders(mongoose_connection, http_message, &extracted_id)) return;
	if (route_order_items(mongoose_connection, http_message, &extracted_id)) return;
	if (mg_http_match_uri(http_message, "/api/ping")) { api_ping(mongoose_connection, http_message); return; }
	cors_reply(mongoose_connection, 404, "Content-Type: text/plain\r\n", "Not Found\n");
}
