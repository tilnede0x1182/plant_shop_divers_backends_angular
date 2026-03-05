#include "OrderItemController.h"
#include <drogon/orm/Mapper.h>

#include <../models/Users.h>
#include <../models/Plants.h>
#include <../models/Orders.h>
#include <../models/OrderItems.h>
using namespace drogon_model::plant_shop_cpp;

using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::OrderItems;

/**
 * Cree une reponse HTTP d erreur JSON.
 *
 * @param code Code HTTP
 * @param msg Message d erreur
 * @return Reponse HTTP formatee
 */
static HttpResponsePtr err(int code, const std::string& msg) {
	auto r = HttpResponse::newHttpJsonResponse({{"error", msg}});
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/**
 * Recupere un article de commande par ID.
 *
 * @param req Requete HTTP (non utilise)
 * @param cb Callback de reponse
 * @param id Identifiant de l article
 */
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

/**
 * Met a jour la quantite d un article.
 *
 * @param req Requete HTTP avec JSON (quantity)
 * @param cb Callback de reponse
 * @param id Identifiant de l article
 */
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

/**
 * Supprime un article de commande.
 *
 * @param req Requete HTTP (non utilise)
 * @param cb Callback de reponse
 * @param id Identifiant de l article
 */
void OrderItemController::deleteOrderItem(const HttpRequestPtr&,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<OrderItems> m(app().getDbClient());
		m.deleteByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse({{"deleted", true}}));
	} catch (...) { cb(err(404, "Item introuvable")); }
}
