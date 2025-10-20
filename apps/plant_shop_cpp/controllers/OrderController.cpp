#include "OrderController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
using namespace drogon;
using namespace drogon::orm;

/**
 * """ Crée une commande avec des items pour l'utilisateur courant """
 */
void OrderController::createOrder(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("items")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Items manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}

	auto cookies = req->cookies();
	if (cookies.find("auth_user") == cookies.end()) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Non connecté"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}
	std::string email = cookies.at("auth_user");

	auto db = app().getDbClient();
	auto u = db->execSqlSync("SELECT id FROM users WHERE email=$1", email);
	if (u->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Utilisateur inconnu"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}
	int userId = (*u)[0]["id"].as<int>();

	Json::Value items = (*json)["items"];
	if (!items.isArray() || items.empty()) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Liste vide"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}

	// Calcul du total
	double total = 0;
	for (const auto& it : items) {
		int plantId = it["plantId"].asInt();
		int qty = it["quantity"].asInt();
		auto p = db->execSqlSync("SELECT price, stock FROM plants WHERE id=$1", plantId);
		if (p->size() == 0 || (*p)[0]["stock"].as<int>() < qty) {
			auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Stock insuffisant"}});
			resp->setStatusCode(k400BadRequest);
			return callback(resp);
		}
		total += (*p)[0]["price"].as<double>() * qty;
	}

	auto order = db->execSqlSync(
	    "INSERT INTO orders (user_id, total, status) VALUES ($1,$2,'pending') RETURNING id",
	    userId, total);
	int orderId = (*order)[0]["id"].as<int>();

	// Insertion des items et mise à jour du stock
	for (const auto& it : items) {
		int plantId = it["plantId"].asInt();
		int qty = it["quantity"].asInt();
		auto priceRow = db->execSqlSync("SELECT price FROM plants WHERE id=$1", plantId);
		double unitPrice = (*priceRow)[0]["price"].as<double>();
		db->execSqlSync("INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES ($1,$2,$3,$4)",
		                orderId, plantId, qty, unitPrice);
		db->execSqlSync("UPDATE plants SET stock = stock - $1 WHERE id=$2", qty, plantId);
	}

	Json::Value respBody;
	respBody["id"] = orderId;
	auto resp = HttpResponse::newHttpJsonResponse(respBody);
	resp->setStatusCode(k201Created);
	callback(resp);
}

/**
 * """ Liste les commandes de l'utilisateur connecté """
 */
void OrderController::listOrders(const HttpRequestPtr& req,
                                 std::function<void(const HttpResponsePtr&)>&& callback) {
	auto cookies = req->cookies();
	if (cookies.find("auth_user") == cookies.end()) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Non connecté"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}
	std::string email = cookies.at("auth_user");

	auto db = app().getDbClient();
	auto u = db->execSqlSync("SELECT id FROM users WHERE email=$1", email);
	if (u->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Utilisateur inconnu"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}
	int userId = (*u)[0]["id"].as<int>();

	auto r = db->execSqlSync("SELECT * FROM orders WHERE user_id=$1 ORDER BY id DESC", userId);
	Json::Value arr(Json::arrayValue);
	for (auto row : *r) {
		Order o;
		o.id = row["id"].as<int>();
		o.user_id = row["user_id"].as<int>();
		o.total = row["total"].as<double>();
		o.status = row["status"].as<std::string>();
		o.created_at = row["created_at"].as<std::string>();
		auto items = db->execSqlSync("SELECT * FROM order_items WHERE order_id=$1", o.id);
		for (auto it : *items) {
			OrderItem oi;
			oi.id = it["id"].as<int>();
			oi.order_id = it["order_id"].as<int>();
			oi.plant_id = it["plant_id"].as<int>();
			oi.quantity = it["quantity"].as<int>();
			oi.price = it["price"].as<double>();
			o.items.push_back(oi);
		}
		arr.append(o.toJson());
	}

	auto resp = HttpResponse::newHttpJsonResponse(arr);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Récupère une commande spécifique """
 */
void OrderController::getOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback, int orderId) {
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT * FROM orders WHERE id=$1", orderId);
	if (r->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Commande introuvable"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}
	Order o;
	o.id = (*r)[0]["id"].as<int>();
	o.user_id = (*r)[0]["user_id"].as<int>();
	o.total = (*r)[0]["total"].as<double>();
	o.status = (*r)[0]["status"].as<std::string>();
	o.created_at = (*r)[0]["created_at"].as<std::string>();

	auto items = db->execSqlSync("SELECT * FROM order_items WHERE order_id=$1", orderId);
	for (auto it : *items) {
		OrderItem oi;
		oi.id = it["id"].as<int>();
		oi.order_id = it["order_id"].as<int>();
		oi.plant_id = it["plant_id"].as<int>();
		oi.quantity = it["quantity"].as<int>();
		oi.price = it["price"].as<double>();
		o.items.push_back(oi);
	}
	auto resp = HttpResponse::newHttpJsonResponse(o.toJson());
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Met à jour le statut d’une commande (admin) """
 */
void OrderController::updateOrder(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback,
                                  int orderId) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("status")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Statut manquant"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}
	auto db = app().getDbClient();
	db->execSqlSync("UPDATE orders SET status=$1 WHERE id=$2", (*json)["status"].asString(), orderId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Supprime une commande (admin) """
 */
void OrderController::deleteOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback, int orderId) {
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM orders WHERE id=$1", orderId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ CRUD basique sur order_items """
 */
void OrderController::getOrderItem(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback, int itemId) {
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT * FROM order_items WHERE id=$1", itemId);
	if (r->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Item introuvable"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}
	OrderItem oi;
	oi.id = (*r)[0]["id"].as<int>();
	oi.order_id = (*r)[0]["order_id"].as<int>();
	oi.plant_id = (*r)[0]["plant_id"].as<int>();
	oi.quantity = (*r)[0]["quantity"].as<int>();
	oi.price = (*r)[0]["price"].as<double>();
	auto resp = HttpResponse::newHttpJsonResponse(oi.toJson());
	resp->setStatusCode(k200OK);
	callback(resp);
}

void OrderController::updateOrderItem(const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& callback, int itemId) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("quantity")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Champs manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}
	auto db = app().getDbClient();
	db->execSqlSync("UPDATE order_items SET quantity=$1 WHERE id=$2", (*json)["quantity"].asInt(), itemId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}

void OrderController::deleteOrderItem(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback, int itemId) {
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM order_items WHERE id=$1", itemId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}
