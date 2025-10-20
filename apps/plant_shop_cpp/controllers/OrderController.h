#pragma once
#include <drogon/drogon.h>
#include <json/json.h>
#include "../models/Order.h"
#include "../models/OrderItem.h"

/**
 * """ Contrôleur des commandes et items
 *  Routes gérées :
 *   - POST /api/orders
 *   - GET  /api/orders
 *   - GET  /api/orders/{id}
 *   - PATCH /api/orders/{id}
 *   - DELETE /api/orders/{id}
 *   - GET /api/order_items/{id}
 *   - PATCH /api/order_items/{id}
 *   - DELETE /api/order_items/{id}
 * """
 */
class OrderController : public drogon::HttpController<OrderController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(OrderController::createOrder, "/api/orders", drogon::Post);
		ADD_METHOD_TO(OrderController::listOrders, "/api/orders", drogon::Get);
		ADD_METHOD_TO(OrderController::getOrder, "/api/orders/{1}", drogon::Get);
		ADD_METHOD_TO(OrderController::updateOrder, "/api/orders/{1}", drogon::Patch);
		ADD_METHOD_TO(OrderController::deleteOrder, "/api/orders/{1}", drogon::Delete);

		ADD_METHOD_TO(OrderController::getOrderItem, "/api/order_items/{1}", drogon::Get);
		ADD_METHOD_TO(OrderController::updateOrderItem, "/api/order_items/{1}", drogon::Patch);
		ADD_METHOD_TO(OrderController::deleteOrderItem, "/api/order_items/{1}", drogon::Delete);
	METHOD_LIST_END

	/** """ Crée une nouvelle commande pour l'utilisateur courant """ */
	void createOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Liste les commandes de l'utilisateur courant """ */
	void listOrders(const drogon::HttpRequestPtr& req,
	                std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Récupère une commande par ID """ */
	void getOrder(const drogon::HttpRequestPtr& req,
	              std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	              int orderId);

	/** """ Met à jour une commande (admin) """ */
	void updateOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int orderId);

	/** """ Supprime une commande (admin) """ */
	void deleteOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int orderId);

	/** """ Récupère un item de commande par ID """ */
	void getOrderItem(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                  int itemId);

	/** """ Met à jour un item de commande (admin) """ */
	void updateOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);

	/** """ Supprime un item de commande """ */
	void deleteOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);
};
