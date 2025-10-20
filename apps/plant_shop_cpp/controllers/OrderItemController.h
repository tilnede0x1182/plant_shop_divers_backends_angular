#pragma once
#include <drogon/drogon.h>
#include <json/json.h>

/**
 * """ Contrôleur des order_items
 *  Routes:
 *   - GET    /api/order_items/{id}
 *   - PATCH  /api/order_items/{id}
 *   - DELETE /api/order_items/{id}
 * """
 */
class OrderItemController : public drogon::HttpController<OrderItemController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(OrderItemController::getOrderItem, "/api/order_items/{1}", drogon::Get);
		ADD_METHOD_TO(OrderItemController::updateOrderItem, "/api/order_items/{1}", drogon::Patch);
		ADD_METHOD_TO(OrderItemController::deleteOrderItem, "/api/order_items/{1}", drogon::Delete);
	METHOD_LIST_END

	/** """ Récupère un item de commande """ */
	void getOrderItem(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                  int itemId);

	/** """ Met à jour un item (quantity uniquement) """ */
	void updateOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);

	/** """ Supprime un item """ */
	void deleteOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);
};
