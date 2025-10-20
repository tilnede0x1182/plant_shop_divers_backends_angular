#include "PlantController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
using namespace drogon;
using namespace drogon::orm;

/**
 * """ Vérifie si l’utilisateur connecté est admin """
 */
static bool isAdmin(const HttpRequestPtr& req) {
	auto cookies = req->cookies();
	if (cookies.find("auth_user") == cookies.end()) return false;
	auto email = cookies.at("auth_user");
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT is_admin FROM users WHERE email=$1", email);
	return (r->size() > 0 && (*r)[0]["is_admin"].as<bool>());
}

/**
 * """ Liste publique des plantes """
 */
void PlantController::listPlants(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback) {
	auto db = app().getDbClient();
	auto rows = db->execSqlSync("SELECT id, name, description, price, stock, created_at FROM plants ORDER BY id ASC");
	Json::Value arr(Json::arrayValue);
	for (auto r : *rows) {
		Json::Value j;
		j["id"] = r["id"].as<int>();
		j["name"] = r["name"].as<std::string>();
		j["description"] = r["description"].as<std::string>();
		j["price"] = r["price"].as<double>();
		j["stock"] = r["stock"].as<int>();
		j["created_at"] = r["created_at"].as<std::string>();
		arr.append(j);
	}
	auto resp = HttpResponse::newHttpJsonResponse(arr);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Détails d'une plante """
 */
void PlantController::getPlant(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& callback, int plantId) {
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT * FROM plants WHERE id=$1", plantId);
	if (r->size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Plante introuvable"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}
	Json::Value j;
	j["id"] = (*r)[0]["id"].as<int>();
	j["name"] = (*r)[0]["name"].as<std::string>();
	j["description"] = (*r)[0]["description"].as<std::string>();
	j["price"] = (*r)[0]["price"].as<double>();
	j["stock"] = (*r)[0]["stock"].as<int>();
	j["created_at"] = (*r)[0]["created_at"].as<std::string>();
	auto resp = HttpResponse::newHttpJsonResponse(j);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Liste complète (admin uniquement) """
 */
void PlantController::listAdminPlants(const HttpRequestPtr& req,
                                      std::function<void(const HttpResponsePtr&)>&& callback) {
	if (!isAdmin(req)) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Accès refusé"}});
		resp->setStatusCode(k403Forbidden);
		return callback(resp);
	}
	auto db = app().getDbClient();
	auto rows = db->execSqlSync("SELECT id, name, description, price, stock, created_at FROM plants ORDER BY name ASC");
	Json::Value arr(Json::arrayValue);
	for (auto r : *rows) {
		Json::Value j;
		j["id"] = r["id"].as<int>();
		j["name"] = r["name"].as<std::string>();
		j["description"] = r["description"].as<std::string>();
		j["price"] = r["price"].as<double>();
		j["stock"] = r["stock"].as<int>();
		j["created_at"] = r["created_at"].as<std::string>();
		arr.append(j);
	}
	auto resp = HttpResponse::newHttpJsonResponse(arr);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Création d'une plante (admin) """
 */
void PlantController::createPlant(const HttpRequestPtr& req, std::function<void(const HttpResponsePtr&)>&& callback) {
	if (!isAdmin(req)) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Non autorisé"}});
		resp->setStatusCode(k403Forbidden);
		return callback(resp);
	}
	auto json = req->getJsonObject();
	if (!json || !json->isMember("name") || !json->isMember("price") || !json->isMember("stock")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Champs manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}
	std::string name = (*json)["name"].asString();
	std::string desc = json->isMember("description") ? (*json)["description"].asString() : "";
	double price = (*json)["price"].asDouble();
	int stock = (*json)["stock"].asInt();
	auto db = app().getDbClient();
	auto r = db->execSqlSync(
	    "INSERT INTO plants (name, description, price, stock) VALUES ($1,$2,$3,$4) RETURNING id",
	    name, desc, price, stock);
	Json::Value j;
	j["id"] = (*r)[0]["id"].as<int>();
	auto resp = HttpResponse::newHttpJsonResponse(j);
	resp->setStatusCode(k201Created);
	callback(resp);
}

/**
 * """ Mise à jour d'une plante (admin) """
 */
void PlantController::updatePlant(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback,
                                  int plantId) {
	if (!isAdmin(req)) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Non autorisé"}});
		resp->setStatusCode(k403Forbidden);
		return callback(resp);
	}
	auto json = req->getJsonObject();
	if (!json || (!json->isMember("name") && !json->isMember("price") && !json->isMember("stock") && !json->isMember("description"))) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Aucun champ modifiable"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}
	std::string query = "UPDATE plants SET ";
	std::vector<std::string> sets;
	Json::Value vals;
	if (json->isMember("name")) sets.push_back("name='" + (*json)["name"].asString() + "'");
	if (json->isMember("description")) sets.push_back("description='" + (*json)["description"].asString() + "'");
	if (json->isMember("price")) sets.push_back("price=" + std::to_string((*json)["price"].asDouble()));
	if (json->isMember("stock")) sets.push_back("stock=" + std::to_string((*json)["stock"].asInt()));
	for (size_t i = 0; i < sets.size(); i++) {
		query += sets[i];
		if (i < sets.size() - 1) query += ", ";
	}
	query += " WHERE id=" + std::to_string(plantId);
	auto db = app().getDbClient();
	db->execSqlSync(query);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Suppression d'une plante (admin) """
 */
void PlantController::deletePlant(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback,
                                  int plantId) {
	if (!isAdmin(req)) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Non autorisé"}});
		resp->setStatusCode(k403Forbidden);
		return callback(resp);
	}
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM plants WHERE id=$1", plantId);
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	resp->setStatusCode(k200OK);
	callback(resp);
}
