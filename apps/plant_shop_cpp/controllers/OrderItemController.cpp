#include "OrderItemController.h"
#include <drogon/orm/Mapper.h>
using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::OrderItems;

/* ---- Réponse d’erreur simplifiée ---- */
static HttpResponsePtr err(int code, const std::string& msg) {
	auto r = HttpResponse::newHttpJsonResponse({{"error", msg}});
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/* ---- Lecture ---- */
void OrderItemController::getOrderItem(const HttpRequestPtr&,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> m(app().getDbClient());
		auto item = m.findByPrimaryKey(id);
		auto r = HttpResponse::newHttpJsonResponse(item.toJson());
		r->setStatusCode(k200OK);
		cb(r);
	} catch (...) { cb(err(404, "Item introuvable")); }
}

/* ---- Mise à jour ---- */
void OrderItemController::updateOrderItem(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	auto j = req->getJsonObject();
	if (!j || !j->isMember("quantity"))
		return cb(err(400, "Champs manquants"));
	try {
		Mapper<OrderItems> m(app().getDbClient());
		auto item = m.findByPrimaryKey(id);
		item.setQuantity(std::max(0, (*j)["quantity"].asInt()));
		m.update(item);
		cb(HttpResponse::newHttpJsonResponse({{"updated", true}}));
	} catch (...) { cb(err(404, "Item introuvable")); }
}

/* ---- Suppression ---- */
void OrderItemController::deleteOrderItem(const HttpRequestPtr&,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> m(app().getDbClient());
		m.deleteByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse({{"deleted", true}}));
	} catch (...) { cb(err(404, "Item introuvable")); }
}
