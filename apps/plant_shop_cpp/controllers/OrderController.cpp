#include "OrderController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
#include "AuthController.h"

#include "../utils/TokenUtils.h"
#include "../models/Users.h"
#include "../models/Plants.h"
#include "../models/Orders.h"
#include "../models/OrderItems.h"
#include <unordered_map>

using namespace drogon_model::plant_shop_cpp;

using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Orders;
using drogon_model::plant_shop_cpp::OrderItems;
using drogon_model::plant_shop_cpp::Users;
using drogon_model::plant_shop_cpp::Plants;

/* ---- Utilitaires internes ---- */

static HttpResponsePtr err(int code, const std::string& msg) {
	Json::Value j;
	j["error"] = msg;
	auto r = HttpResponse::newHttpJsonResponse(j);
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/* ---- Créer une commande ---- */
void OrderController::createOrder(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		Json::Value j; auto json = req->getJsonObject();
		if (!json || !json->isMember("items")) return cb(err(400,"Items manquants"));
		auto user = AuthController::canActDecodeJWT(req); if (!user) return cb(err(401,"Non connecté"));

		Mapper<Plants> mp(app().getDbClient());
		Mapper<Orders> mo(app().getDbClient());
		Mapper<OrderItems> mi(app().getDbClient());

		double total = 0;
		for (auto &it : (*json)["items"]) {
			auto plant = mp.findByPrimaryKey(it["plantId"].asInt());
			int qty = it["quantity"].asInt();
			if (plant.getValueOfStock() < qty) return cb(err(400,"Stock insuffisant"));
			total += std::stod(plant.getValueOfPrice()) * qty;
		}

		Orders o;
		o.setUserId(user->getValueOfId());
		o.setTotal(std::to_string(total));
		o.setStatus("pending");
		mo.insert(o);

		for (auto &it : (*json)["items"]) {
			auto plant = mp.findByPrimaryKey(it["plantId"].asInt());
			int qty = it["quantity"].asInt();
			OrderItems oi;
			oi.setOrderId(o.getValueOfId());
			oi.setPlantId(plant.getValueOfId());
			oi.setQuantity(qty);
			oi.setPrice(plant.getValueOfPrice());
			mi.insert(oi);
			plant.setStock(plant.getValueOfStock() - qty);
			mp.update(plant);
		}

		Json::Value resp;
		resp["id"] = o.getValueOfId();
		auto r = HttpResponse::newHttpJsonResponse(resp);
		r->setStatusCode(k201Created);
		cb(r);
	} catch (...) { cb(err(500,"Erreur serveur")); }
}

/* ---- Liste commandes utilisateur ---- */
void OrderController::listOrders(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		auto user = AuthController::canActDecodeJWT(req);
		if (!user) return cb(err(401, "Non connecté"));

		auto db = app().getDbClient();
		Mapper<Orders> mo(db);
		Mapper<OrderItems> mi(db);
		Mapper<Plants> mp(db);

		// récupère toutes les commandes de l'utilisateur
		auto orders = mo.findBy(Criteria(Orders::Cols::_user_id, user->getValueOfId()));
		if (orders.empty()) {
			auto emptyR = HttpResponse::newHttpJsonResponse(Json::Value(Json::arrayValue));
			emptyR->setStatusCode(k200OK);
			return cb(emptyR);
		}

		// 1) numérotation chronologique (ancienne -> 1 ... récente -> N)
		auto ordersChron = orders; // copie
		std::sort(ordersChron.begin(), ordersChron.end(),
		          [](const Orders &a, const Orders &b) {
			          return a.getValueOfCreatedAt() < b.getValueOfCreatedAt();
		          });
		std::unordered_map<int32_t, int> chronoIdx;
		int idx = 1;
		for (auto &o : ordersChron) {
			chronoIdx[o.getValueOfId()] = idx++;
		}

		// 2) affichage : récentes d'abord (descendant)
		auto ordersDesc = orders; // copie
		std::sort(ordersDesc.begin(), ordersDesc.end(),
		          [](const Orders &a, const Orders &b) {
			          return b.getValueOfCreatedAt() < a.getValueOfCreatedAt();
		          });

		Json::Value arr(Json::arrayValue);
		for (auto &o : ordersDesc) {
			auto jsonOrder = o.toJson();
			// injecte la numérotation chronologique (1 = plus ancienne)
			jsonOrder["chronological_number"] = chronoIdx[o.getValueOfId()];

			// items
			auto items = mi.findBy(Criteria(OrderItems::Cols::_order_id, o.getValueOfId()));
			Json::Value jItems(Json::arrayValue);
			for (auto &it : items) {
				auto jItem = it.toJson();
				auto plant = mp.findByPrimaryKey(it.getValueOfPlantId());
				jItem["plant"] = plant.toJson();
				jItems.append(jItem);
			}
			jsonOrder["orderItems"] = jItems;
			arr.append(jsonOrder);
		}

		auto r = HttpResponse::newHttpJsonResponse(arr);
		r->setStatusCode(k200OK);
		cb(r);
	} catch (...) {
		cb(err(500, "Erreur serveur"));
	}
}

/* ---- Lecture commande ---- */
void OrderController::getOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<Orders> mo(app().getDbClient());
		auto o = mo.findByPrimaryKey(id);
		auto r = HttpResponse::newHttpJsonResponse(o.toJson());
		r->setStatusCode(k200OK);
		cb(r);
	} catch (...) { cb(err(404,"Commande introuvable")); }
}

/* ---- MAJ commande ---- */
void OrderController::updateOrder(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	auto j = req->getJsonObject();
	if (!j || !j->isMember("status")) return cb(err(400,"Statut manquant"));
	try {
		Mapper<Orders> mo(app().getDbClient());
		auto o = mo.findByPrimaryKey(id);
		o.setStatus((*j)["status"].asString());
		mo.update(o);
		Json::Value resp;
		resp["updated"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404,"Commande introuvable")); }
}

/* ---- Suppression commande ---- */
void OrderController::deleteOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<Orders> mo(app().getDbClient());
		mo.deleteByPrimaryKey(id);
		Json::Value resp;
		resp["deleted"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404,"Commande introuvable")); }
}

/* ---- CRUD items ---- */
void OrderController::getOrderItem(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> mi(app().getDbClient());
		auto it = mi.findByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse(it.toJson()));
	} catch (...) { cb(err(404,"Item introuvable")); }
}

void OrderController::updateOrderItem(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	auto j = req->getJsonObject();
	if (!j || !j->isMember("quantity")) return cb(err(400,"Champs manquants"));
	try {
		Mapper<OrderItems> mi(app().getDbClient());
		auto it = mi.findByPrimaryKey(id);
		it.setQuantity((*j)["quantity"].asInt());
		mi.update(it);
		Json::Value resp;
		resp["updated"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404,"Item introuvable")); }
}

void OrderController::deleteOrderItem(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> mi(app().getDbClient());
		mi.deleteByPrimaryKey(id);
		Json::Value resp;
		resp["deleted"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404,"Item introuvable")); }
}
