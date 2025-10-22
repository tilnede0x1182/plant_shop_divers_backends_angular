#include "PlantController.h"
#include "AuthController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>

#include "../models/Users.h"
#include "../models/Plants.h"
#include "../models/Orders.h"
#include "../models/OrderItems.h"
using namespace drogon_model::plant_shop_cpp;

using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Plants;
using drogon_model::plant_shop_cpp::Users;

/* ---- Helpers ---- */
static HttpResponsePtr err(int code, const std::string &msg) {
	Json::Value j;
	j["error"] = msg;
	auto r = HttpResponse::newHttpJsonResponse(j);
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/* ---- Liste publique ---- */
void PlantController::listPlants(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto all = mp.findAll();
		Json::Value arr(Json::arrayValue);
		for (auto &p : all) arr.append(p.toJson());
		auto r = HttpResponse::newHttpJsonResponse(arr);
		r->setStatusCode(k200OK);
		cb(r);
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
	if (!AuthController::isAdmin(req)) return cb(err(403, "Accès refusé"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		auto all = mp.findAll();
		std::sort(all.begin(), all.end(), [](const Plants &a, const Plants &b) {
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
	if (!AuthController::isAdmin(req)) return cb(err(403, "Non autorisé"));
	auto j = req->getJsonObject();
	if (!j || !j->isMember("name") || !j->isMember("price") || !j->isMember("stock"))
		return cb(err(400, "Champs manquants"));
	try {
		Mapper<Plants> mp(app().getDbClient());
		Plants p;
		p.setName((*j)["name"].asString());
		if (j->isMember("description")) p.setDescription((*j)["description"].asString());
		p.setPrice(std::to_string((*j)["price"].asInt()));
		p.setStock((*j)["stock"].asInt());
		mp.insert(p);
		Json::Value resp;
		resp["id"] = p.getValueOfId();
		auto r = HttpResponse::newHttpJsonResponse(resp);
		r->setStatusCode(k201Created);
		cb(r);
	} catch (...) { cb(err(500, "Erreur création plante")); }
}

/* ---- Mise à jour ---- */
void PlantController::updatePlant(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {

	LOG_INFO << "[PlantController] --- Début PATCH /admin/plants/" << id << " ---";
	std::cout << "[PlantController] >>> Début PATCH /admin/plants/" << id << std::endl;

	// Diagnostic cookies
	auto ck = req->cookies();
	LOG_INFO << "[PlantController] Cookies reçus (" << ck.size() << "):";
	std::cout << "[PlantController] >>> Nombre de cookies=" << ck.size() << std::endl;
	for (auto &[k, v] : ck) {
		LOG_INFO << "  " << k << "=" << v;
		std::cout << "[PlantController] >>> Cookie " << k << "=" << v << std::endl;
	}

	// Vérification d’administration
	LOG_INFO << "[PlantController] Appel imminent à isAdmin(req)";
	std::cout << "[PlantController] >>> Appel isAdmin(req)" << std::endl;

	bool admin = false;
	try {
		admin = AuthController::isAdmin(req);
		LOG_INFO << "[PlantController] Résultat isAdmin=" << admin;
		std::cout << "[PlantController] >>> Résultat isAdmin=" << admin << std::endl;
	} catch (const std::exception &ex) {
		LOG_ERROR << "[PlantController] Exception pendant isAdmin(): " << ex.what();
		std::cout << "[PlantController] >>> Exception isAdmin: " << ex.what() << std::endl;
		return cb(err(500, "Erreur interne pendant vérif admin"));
	}

	if (!admin) {
		LOG_WARN << "⛔ Accès refusé à /admin/plants/" << id
		         << " — utilisateur non admin ou cookie absent";
		std::cout << "[PlantController] >>> Accès refusé /admin/plants/" << id << std::endl;
		return cb(err(403, "Non autorisé"));
	}

	LOG_INFO << "[PlantController] Accès autorisé, poursuite de la mise à jour";
	std::cout << "[PlantController] >>> Accès autorisé" << std::endl;

	// Vérification du JSON
	auto j = req->getJsonObject();
	if (!j || j->empty()) {
		LOG_WARN << "[PlantController] JSON vide reçu pour PATCH id=" << id;
		std::cout << "[PlantController] >>> JSON vide" << std::endl;
		return cb(err(400, "Aucun champ modifiable"));
	}

	try {
		Mapper<Plants> mp(app().getDbClient());
		auto p = mp.findByPrimaryKey(id);
		if (j->isMember("name")) p.setName((*j)["name"].asString());
		if (j->isMember("description")) p.setDescription((*j)["description"].asString());
		if (j->isMember("price")) p.setPrice(std::to_string((*j)["price"].asInt()));
		if (j->isMember("stock")) p.setStock((*j)["stock"].asInt());
		mp.update(p);
		Json::Value resp;
		resp["updated"] = true;
		LOG_INFO << "[PlantController] Mise à jour terminée pour id=" << id;
		std::cout << "[PlantController] >>> Update terminé id=" << id << std::endl;
		cb(HttpResponse::newHttpJsonResponse(resp));
	}
	catch (const DrogonDbException &ex) {
		LOG_ERROR << "[PlantController] Erreur DB: " << ex.base().what();
		std::cout << "[PlantController] >>> Erreur DB: " << ex.base().what() << std::endl;
		cb(err(500, "Erreur base de données"));
	}
	catch (...) {
		LOG_ERROR << "[PlantController] Exception inconnue dans updatePlant()";
		std::cout << "[PlantController] >>> Exception inconnue" << std::endl;
		cb(err(404, "Plante introuvable"));
	}
}

/* ---- Suppression ---- */
void PlantController::deletePlant(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int id) {
	// --- Vérification des droits administrateur (avec logs complets) ---
	LOG_INFO << "[PlantController] Vérification admin avant accès à " << req->path();
	std::cout << "[PlantController] >>> Vérification admin avant " << req->path() << std::endl;

	auto resp = HttpResponse::newHttpResponse();

	try {
		bool admin = AuthController::isAdmin(req);
		LOG_INFO << "[PlantController] Appel isAdmin() terminé → " << admin;
		std::cout << "[PlantController] >>> Résultat isAdmin=" << admin << std::endl;

		if (!admin) {
			LOG_WARN << "⛔ Accès refusé à " << req->path()
			         << " — utilisateur non admin ou cookie absent";
			std::cout << "[PlantController] >>> Rejet accès " << req->path() << std::endl;
			resp->setStatusCode(k403Forbidden);
			resp->setBody(R"({"error":"Non autorisé"})");
			cb(resp);
			return;
		}

		LOG_INFO << "[PlantController] Accès autorisé à " << req->path();
		std::cout << "[PlantController] >>> Accès autorisé " << req->path() << std::endl;
	}
	catch (const std::exception& ex) {
		LOG_ERROR << "[PlantController] Exception pendant isAdmin() : " << ex.what();
		std::cout << "[PlantController] >>> Exception " << ex.what() << std::endl;
		resp->setStatusCode(k500InternalServerError);
		resp->setBody(R"({"error":"Erreur serveur"})");
		cb(resp);
		return;
	}

	try {
		Mapper<Plants> mp(app().getDbClient());
		mp.deleteByPrimaryKey(id);
		Json::Value respJson;
		respJson["deleted"] = true;
		cb(HttpResponse::newHttpJsonResponse(respJson));
	} catch (...) {
		cb(err(404, "Plante introuvable"));
	}
}
