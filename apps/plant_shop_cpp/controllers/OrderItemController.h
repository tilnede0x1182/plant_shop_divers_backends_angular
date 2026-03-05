#pragma once
#include <drogon/drogon.h>
#include <json/json.h>

/**
 * Controleur des articles de commande.
 * Routes gerees : CRUD /api/order_items.
 */
class OrderItemController : public drogon::HttpController<OrderItemController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(OrderItemController::getOrderItem, "/api/order_items/{1}", drogon::Get);
		ADD_METHOD_TO(OrderItemController::updateOrderItem, "/api/order_items/{1}", drogon::Patch);
		ADD_METHOD_TO(OrderItemController::deleteOrderItem, "/api/order_items/{1}", drogon::Delete);
	METHOD_LIST_END

	/**
 * Recupere un item de commande.
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 * @param itemId Identifiant de l item
 */
	void getOrderItem(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                  int itemId);

	/**
 * Met a jour un item (quantity uniquement).
 *
 * @param req Requete HTTP avec JSON
 * @param callback Callback de reponse
 * @param itemId Identifiant de l item
 */
	void updateOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);

	/**
 * Supprime un item.
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 * @param itemId Identifiant de l item
 */
	void deleteOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);
};
