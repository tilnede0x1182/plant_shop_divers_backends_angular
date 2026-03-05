/* ==============================================================================
   Importations
   ============================================================================== */
#include "order_item_controller.h"
#include <cjson/cJSON.h>
#include <string.h>
#include <stdlib.h>
#include "../repository/order_item_repository.h"
#include "../repository/order_repository.h"
#include "../repository/user_repository.h"
#include "mongoose/mongoose.h"
#include <stdint.h>
#include "../utils/utils.h"

/* ==============================================================================
   Données
   ============================================================================== */
extern PGconn* DATABASE_CONNECTION;

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/**
 * Envoie une réponse JSON au client et libère la mémoire.
 *
 * @param mongoose_connection Connexion mongoose
 * @param json_obj Objet cJSON à envoyer (sera libéré)
 * @param code Code HTTP de réponse
 */
static void send_json_reply(struct mg_connection* mongoose_connection, cJSON* json_obj, int http_code) {
    char *json_text = cJSON_PrintUnformatted(json_obj);
    mg_http_reply(mongoose_connection, http_code, "Content-Type: application/json\r\n", "%s", json_text);
    free(json_text);
    if (json_obj) cJSON_Delete(json_obj);
}

/**
 * Vérifie si l'utilisateur courant est administrateur.
 *
 * @param http_message Message HTTP contenant les cookies
 * @return 1 si admin, 0 sinon
 */
static int is_admin(struct mg_http_message* http_message) {
    int user_identifier = get_current_user_id(http_message);
    return user_repo_is_admin(DATABASE_CONNECTION, user_identifier);
}

/**
 * Callback pour ajouter un OrderItem au tableau JSON.
 *
 * @param order_item Pointeur vers l'OrderItem à convertir
 * @param destination_array Pointeur vers le tableau cJSON de destination
 */
static void order_items_by_order_cb(OrderItem *order_item, void *destination_array) {
    cJSON *json_array = (cJSON*)destination_array;
    cJSON* json_obj = cJSON_CreateObject();
    cJSON_AddNumberToObject(json_obj, "id", order_item->id);
    cJSON_AddNumberToObject(json_obj, "plantId", order_item->plant_id);
    cJSON_AddNumberToObject(json_obj, "quantity", order_item->qty);
    cJSON_AddNumberToObject(json_obj, "price", order_item->price);
    cJSON_AddItemToArray(json_array, json_obj);
}

/* ==============================================================================
   Fonctions principales
   ============================================================================== */
/**
 * Récupère tous les articles d'une commande.
 * Vérifie les droits d'accès (admin ou propriétaire).
 *
 * @param mongoose_connection Connexion mongoose
 * @param http_message Message HTTP de la requête
 * @param order_id ID de la commande
 */
void order_items_by_order(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int order_identifier) {
    int user_identifier = get_current_user_id(http_message);
    if (!user_identifier) {
        mg_http_reply(mongoose_connection, 401, "", "");
        return;
    }
    if (!is_admin(http_message) && !order_repo_belongs_to(DATABASE_CONNECTION, order_identifier, user_identifier)) {
        mg_http_reply(mongoose_connection, 403, "", "");
        return;
    }
    cJSON *items_array = cJSON_CreateArray();
    order_item_repo_by_order(DATABASE_CONNECTION, order_identifier, order_items_by_order_cb, items_array);
    send_json_reply(mongoose_connection, items_array, 200);
}

/**
 * Modifie un article de commande (admin uniquement).
 *
 * @param mongoose_connection Connexion mongoose
 * @param http_message Message HTTP contenant le JSON de mise à jour
 * @param item_identifier ID de l'article à modifier
 */
void order_item_patch(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int item_identifier) {
    if (!is_admin(http_message)) {
        mg_http_reply(mongoose_connection, 403, "", "");
        return;
    }

    cJSON *update_data = cJSON_ParseWithLength(http_message->body.buf, http_message->body.len);
    if (!update_data) {
        mg_http_reply(mongoose_connection, 400, "Content-Type: application/json\r\n", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    order_item_repo_patch(DATABASE_CONNECTION, item_identifier, update_data);
    cJSON_Delete(update_data);
    mg_http_reply(mongoose_connection, 200, "", "");
}

/**
 * Supprime un article de commande (admin uniquement).
 *
 * @param mongoose_connection Connexion mongoose
 * @param http_message Message HTTP de la requête
 * @param item_identifier ID de l'article à supprimer
 */
void order_item_del(struct mg_connection *mongoose_connection, struct mg_http_message *http_message, int item_identifier) {
    if (!is_admin(http_message)) {
        mg_http_reply(mongoose_connection, 403, "", "");
        return;
    }
    order_item_repo_del(DATABASE_CONNECTION, item_identifier);
    mg_http_reply(mongoose_connection, 200, "", "");
}
