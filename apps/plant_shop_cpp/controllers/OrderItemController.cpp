#include "OrderItemController.h"
#include <drogon/orm/Mapper.h>
using namespace drogon;
using namespace drogon::orm;

/**
 * """ Lecture d'un order_item par id """
 */
void OrderItemController::getOrderItem(const HttpRequestPtr&,
                                       std::function<void(const HttpResponsePtr&)>&& callback,
                                       int itemId) {
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT id, order_id, plant_id, quantity, price FROM order_items WHERE id=$1", itemId);
	if (r->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Item introuvable"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}
	Json::Value j;
	j["id"] = (*r)[0]["id"].as<int>();
	j["orderId"] = (*r)[0]["order_id"].as<int>();
	j["plantId"] = (*r)[0]["plant_id"].as<int>();
	j["quantity"] = (*r)[0]["quantity"].as<int>();
	j["price"] = (*r)[0]["price"].as<double>();
	auto resp = HttpResponse::newHttpJsonResponse(j);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Mise à jour d'un order_item (quantity) """
 */
void OrderItemController::updateOrderItem(const HttpRequestPtr& req,
                                          std::function<void(const HttpResponsePtr&)>&& callback,
                                          int itemId) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("quantity")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Champs manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}
	int quantity = std::max(0, (*json)["quantity"].asInt());
	auto db = app().getDbClient();

	// Existence
	auto r = db->execSqlSync("SELECT id FROM order_items WHERE id=$1", itemId);
	if (r->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Item introuvable"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}

	db->execSqlSync("UPDATE order_items SET quantity=$1 WHERE id=$2", quantity, itemId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Suppression d'un order_item """
 */
void OrderItemController::deleteOrderItem(const HttpRequestPtr&,
                                          std::function<void(const HttpResponsePtr&)>&& callback,
                                          int itemId) {
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM order_items WHERE id=$1", itemId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}
