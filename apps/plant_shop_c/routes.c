#include <stdio.h>
#include <string.h>
#include "mongoose/mongoose.h"
#include "routes.h"
#include "src/controllers/auth_controller.h"
#include "src/controllers/plant_controller.h"
#include "src/controllers/user_controller.h"
#include "src/controllers/order_controller.h"
#include "src/controllers/order_item_controller.h"

// Handler basique pour /api/ping
static void api_ping(struct mg_connection *c, struct mg_http_message *hm) {
    mg_http_reply(c, 200, "", "");
    (void)hm;
}

// Fonction de routage principale
void route_request(struct mg_connection *c, struct mg_http_message *hm) {
    int id = 0; // Pour stocker les IDs extraits de l'URL

    // Auth routes
    if (mg_http_match_uri(hm, "/api/auth/login")) {
        auth_login(c, hm);
    } else if (mg_http_match_uri(hm, "/api/auth/register")) {
        auth_register(c, hm);
    } else if (mg_http_match_uri(hm, "/api/auth/me")) {
        auth_me(c, hm);
    }
    // Plant routes
    else if (sscanf(hm->uri.buf, "/api/plants/%d", &id) == 1) {
        plant_get(c, hm, id);
    } else if (mg_http_match_uri(hm, "/api/admin/plants")) {
        if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
            admin_plants_list(c, hm);
        } else if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
            admin_plants_add(c, hm);
        }
    } else if (sscanf(hm->uri.buf, "/api/admin/plants/%d", &id) == 1) {
        if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
            admin_plants_patch(c, hm, id);
        } else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
            admin_plants_del(c, hm, id);
        }
    }
    // User routes
    else if (mg_http_match_uri(hm, "/api/users")) {
         if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
            user_create(c, hm);
        }
    } else if (sscanf(hm->uri.buf, "/api/users/%d", &id) == 1) {
        if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
            user_get(c, hm, id);
        } else if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
            user_patch(c, hm, id);
        } else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
            user_del(c, hm, id);
        }
    } else if (mg_http_match_uri(hm, "/api/admin/users")) {
        admin_users_list(c, hm);
    }
    // Order routes
    else if (mg_http_match_uri(hm, "/api/orders")) {
        if (mg_strcmp(hm->method, mg_str("GET")) == 0) {
            orders_list(c, hm);
        } else if (mg_strcmp(hm->method, mg_str("POST")) == 0) {
            orders_create(c, hm);
        }
    } else if (sscanf(hm->uri.buf, "/api/orders/%d/items", &id) == 1) {
        order_items_by_order(c, hm, id);
    } else if (sscanf(hm->uri.buf, "/api/orders/%d", &id) == 1) {
        if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
            orders_patch(c, hm, id);
        } else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
            orders_del(c, hm, id);
        }
    }
    // Order Item routes
    else if (sscanf(hm->uri.buf, "/api/order-items/%d", &id) == 1) {
        if (mg_strcmp(hm->method, mg_str("PATCH")) == 0) {
            order_item_patch(c, hm, id);
        } else if (mg_strcmp(hm->method, mg_str("DELETE")) == 0) {
            order_item_del(c, hm, id);
        }
    }
    // Ping & 404
    else if (mg_http_match_uri(hm, "/api/ping")) {
        api_ping(c, hm);
    } else {
        mg_http_reply(c, 404, "", "Not Found\n");
    }
}
