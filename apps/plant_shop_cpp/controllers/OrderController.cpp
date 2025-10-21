#include "OrderController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Orders;
using drogon_model::plant_shop_cpp::OrderItems;
using drogon_model::plant_shop_cpp::Users;
using drogon_model::plant_shop_cpp::Plants;

/* ---- Utilitaires internes ---- */
static std::optional<Users> getUserByCookie(const HttpRequestPtr& req) {
	if (!req->cookies().count("auth_user")) return std::nullopt;
	Mapper<Users> mu(app().getDbClient());
	return mu.findOne(Criteria(Users::Cols::_email, req->cookies().at("auth_user")));
}

static HttpResponsePtr err(int code, const std::string& msg) {
	auto r = HttpResponse::newHttpJsonResponse({{"error", msg}});
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/* ---- Créer une commande ---- */
void OrderController::createOrder(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		Json::Value j; auto json = req->getJsonObject();
		if (!json || !json->isMember("items")) return cb(err(400,"Items manquants"));
		auto user = getUserByCookie(req); if (!user) return cb(err(401,"Non connecté"));

		Mapper<Plants> mp(app().getDbClient());
		Mapper<Orders> mo(app().getDbClient());
		Mapper<OrderItems> mi(app().getDbClient());

		double total = 0;
		for (auto &it : (*json)["items"]) {
			auto plant = mp.findByPrimaryKey(it["plantId"].asInt());
			int qty = it["quantity"].asInt();
			if (plant.getValueOfStock() < qty) return cb(err(400,"Stock insuffisant"));
			total += plant.getValueOfPrice() * qty;
		}

		Orders o; o.setUserId(user->getValueOfId());
		o.setTotal(total); o.setStatus("pending");
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

		auto r = HttpResponse::newHttpJsonResponse({{"id", o.getValueOfId()}});
		r->setStatusCode(k201Created); cb(r);
	} catch (...) { cb(err(500,"Erreur serveur")); }
}

/* ---- Liste commandes utilisateur ---- */
void OrderController::listOrders(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		auto user = getUserByCookie(req); if (!user) return cb(err(401,"Non connecté"));
		Mapper<Orders> mo(app().getDbClient());
		auto list = mo.findBy(Criteria(Orders::Cols::_user_id, user->getValueOfId()));
		Json::Value arr(Json::arrayValue);
		for (auto &o : list) arr.append(o.toJson());
		auto r = HttpResponse::newHttpJsonResponse(arr);
		r->setStatusCode(k200OK); cb(r);
	} catch (...) { cb(err(500,"Erreur serveur")); }
}

/* ---- Lecture commande ---- */
void OrderController::getOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<Orders> mo(app().getDbClient());
		auto o = mo.findByPrimaryKey(id);
		auto r = HttpResponse::newHttpJsonResponse(o.toJson());
		r->setStatusCode(k200OK); cb(r);
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
		cb(HttpResponse::newHttpJsonResponse({{"updated",true}}));
	} catch (...) { cb(err(404,"Commande introuvable")); }
}

/* ---- Suppression commande ---- */
void OrderController::deleteOrder(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<Orders> mo(app().getDbClient());
		mo.deleteByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse({{"deleted",true}}));
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
		cb(HttpResponse::newHttpJsonResponse({{"updated",true}}));
	} catch (...) { cb(err(404,"Item introuvable")); }
}

void OrderController::deleteOrderItem(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> mi(app().getDbClient());
		mi.deleteByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse({{"deleted",true}}));
	} catch (...) { cb(err(404,"Item introuvable")); }
}
