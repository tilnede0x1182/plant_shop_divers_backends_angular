#include "PlantController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Plants;
using drogon_model::plant_shop_cpp::Users;

/* ---- Helpers ---- */
static HttpResponsePtr err(int code, const std::string &msg) {
	auto r = HttpResponse::newHttpJsonResponse({{"error", msg}});
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

static bool isAdmin(const HttpRequestPtr &req) {
	try {
		if (!req->cookies().count("auth_user")) return false;
		Mapper<Users> mu(app().getDbClient());
		auto u = mu.findOne(Criteria(Users::Cols::_email, req->cookies().at("auth_user")));
		return u && u->getValueOfIsAdmin();
	} catch (...) { return false; }
}

/* ---- Liste publique ---- */
void PlantController::listPlants(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto all = mp.findAll();
		Json::Value arr(Json::arrayValue);
		for (auto &p : all) arr.append(p.toJson());
		auto r = HttpResponse::newHttpJsonResponse(arr);
		r->setStatusCode(k200OK); cb(r);
	} catch (...) { cb(err(500, "Erreur serveur")); }
}

/* ---- Détails ---- */
void PlantController::getPlant(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto p = mp.findByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse(p.toJson()));
	} catch (...) { cb(err(404, "Plante introuvable")); }
}

/* ---- Liste admin ---- */
void PlantController::listAdminPlants(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	if (!isAdmin(req)) return cb(err(403, "Accès refusé"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto all = mp.findAll();
		std::sort(all.begin(), all.end(), [](const Plants&a,const Plants&b){
			return a.getValueOfName() < b.getValueOfName();
		});
		Json::Value arr(Json::arrayValue);
		for (auto &p : all) arr.append(p.toJson());
		cb(HttpResponse::newHttpJsonResponse(arr));
	} catch (...) { cb(err(500, "Erreur serveur")); }
}

/* ---- Création ---- */
void PlantController::createPlant(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	if (!isAdmin(req)) return cb(err(403, "Non autorisé"));
	auto j = req->getJsonObject();
	if (!j || !j->isMember("name") || !j->isMember("price") || !j->isMember("stock"))
		return cb(err(400, "Champs manquants"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		Plants p;
		p.setName((*j)["name"].asString());
		if (j->isMember("description")) p.setDescription((*j)["description"].asString());
		p.setPrice((*j)["price"].asInt());
		p.setStock((*j)["stock"].asInt());
		mp.insert(p);
		cb(HttpResponse::newHttpJsonResponse({{"id", p.getValueOfId()}}));
	} catch (...) { cb(err(500, "Erreur création plante")); }
}

/* ---- Mise à jour ---- */
void PlantController::updatePlant(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	if (!isAdmin(req)) return cb(err(403, "Non autorisé"));
	auto j = req->getJsonObject();
	if (!j || j->empty()) return cb(err(400, "Aucun champ modifiable"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto p = mp.findByPrimaryKey(id);
		if (j->isMember("name")) p.setName((*j)["name"].asString());
		if (j->isMember("description")) p.setDescription((*j)["description"].asString());
		if (j->isMember("price")) p.setPrice((*j)["price"].asInt());
		if (j->isMember("stock")) p.setStock((*j)["stock"].asInt());
		mp.update(p);
		cb(HttpResponse::newHttpJsonResponse({{"updated", true}}));
	} catch (...) { cb(err(404, "Plante introuvable")); }
}

/* ---- Suppression ---- */
void PlantController::deletePlant(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	if (!isAdmin(req)) return cb(err(403, "Non autorisé"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		mp.deleteByPrimaryKey(id);
		cb(HttpResponse::newHttpJsonResponse({{"deleted", true}}));
	} catch (...) { cb(err(404, "Plante introuvable")); }
}
