#pragma once
#include <drogon/drogon.h>
#include <json/json.h>

/**
 * Controleur des commandes et items.
 * Routes gerees : CRUD /api/orders et /api/order_items.
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

	/**
	 * Cree une nouvelle commande pour l utilisateur courant.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void createOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
	 * Liste les commandes de l utilisateur courant.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void listOrders(const drogon::HttpRequestPtr& req,
	                std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
	 * Recupere une commande par ID.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param orderId ID de la commande
	 */
	void getOrder(const drogon::HttpRequestPtr& req,
	              std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	              int orderId);

	/**
	 * Met a jour une commande (admin).
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param orderId ID de la commande
	 */
	void updateOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int orderId);

	/**
	 * Supprime une commande (admin).
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param orderId ID de la commande
	 */
	void deleteOrder(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int orderId);

	/**
	 * Recupere un item de commande par ID.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param itemId ID de l item
	 */
	void getOrderItem(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                  int itemId);

	/**
	 * Met a jour un item de commande (admin).
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param itemId ID de l item
	 */
	void updateOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);

	/**
	 * Supprime un item de commande.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 * @param itemId ID de l item
	 */
	void deleteOrderItem(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                     int itemId);
};
