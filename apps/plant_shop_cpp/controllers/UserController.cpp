#include "UserController.h"
#include <drogon/orm/Mapper.h>
using namespace drogon;
using namespace drogon::orm;

/** """ Email du user connecté depuis le cookie """ */
static std::optional<std::string> currentEmail(const HttpRequestPtr& req) {
	auto c = req->cookies();
	if (c.find("auth_user") == c.end()) return std::nullopt;
	return c.at("auth_user");
}

/** """ true si connecté """ */
static bool isLogged(const HttpRequestPtr& req) {
	return currentEmail(req).has_value();
}

/** """ true si admin """ */
static bool isAdmin(const HttpRequestPtr& req) {
	auto e = currentEmail(req); if (!e) return false;
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT is_admin FROM users WHERE email=$1", *e);
	return r->size() > 0 && (*r)[0]["is_admin"].as<bool>();
}

/** """ id du user courant """ */
static std::optional<int> currentUserId(const HttpRequestPtr& req) {
	auto e = currentEmail(req); if (!e) return std::nullopt;
	auto db = app().getDbClient();
	auto r = db->execSqlSync("SELECT id FROM users WHERE email=$1", *e);
	if (r->size() == 0) return std::nullopt;
	return (*r)[0]["id"].as<int>();
}

/** """ owner ou admin ? """ */
static bool canActOn(const HttpRequestPtr& req, int userId) {
	if (isAdmin(req)) return true;
	auto id = currentUserId(req);
	return id.has_value() && id.value() == userId;
}

/** """ JSON d'un user sans champs sensibles """ */
static Json::Value userRowToJson(const drogon::orm::Result& rs, size_t i) {
	Json::Value j;
	j["id"] = rs[i]["id"].as<int>();
	j["email"] = rs[i]["email"].as<std::string>();
	j["name"] = rs[i].contains("username") ? rs[i]["username"].as<std::string>() : rs[i]["name"].as<std::string>();
	j["admin"] = rs[i].contains("is_admin") ? rs[i]["is_admin"].as<bool>() : rs[i]["admin"].as<bool>();
	return j;
}

/** """ Création d'utilisateur (admin) """ */
void UserController::createUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	if (!isAdmin(req)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto body = req->getJsonObject();
	if (!body || !(*body).isMember("email") || !(*body).isMember("password"))
	{
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Champs manquants"}});
		r->setStatusCode(k400BadRequest); return cb(r);
	}
	auto db = app().getDbClient();
	std::string email = (*body)["email"].asString();
	std::string name  = (*body).isMember("name") ? (*body)["name"].asString() : (*body)["username"].asString();
	bool adminFlag = (*body).isMember("admin") ? (*body)["admin"].asBool() : (*body)["is_admin"].asBool();
	std::string hash = drogon::utils::getMd5((*body)["password"].asString());

	auto res = db->execSqlSync(
		"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,$4) RETURNING id",
		email, name, hash, adminFlag
	);
	Json::Value j; j["id"] = res->get(0)["id"].as<int>();
	auto r = HttpResponse::newHttpJsonResponse(j);
	r->setStatusCode(k201Created); cb(r);
}

/** """ Liste des utilisateurs (admin) """ */
void UserController::listUsers(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	if (!isAdmin(req)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto db = app().getDbClient();
	auto rows = db->execSqlSync("SELECT id,email,username AS name,is_admin AS admin FROM users ORDER BY id ASC");
	Json::Value arr(Json::arrayValue);
	for (size_t i=0;i<rows->size();i++) arr.append(userRowToJson(*rows, i));
	auto r = HttpResponse::newHttpJsonResponse(arr);
	r->setStatusCode(k200OK); cb(r);
}

/** """ Lecture d’un utilisateur (owner/admin) """ */
void UserController::getUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int userId) {
	if (!isLogged(req) || !canActOn(req, userId)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto db = app().getDbClient();
	auto rows = db->execSqlSync("SELECT id,email,username AS name,is_admin AS admin FROM users WHERE id=$1", userId);
	if (rows->size()==0) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Not found"}});
		r->setStatusCode(k404NotFound); return cb(r);
	}
	auto r = HttpResponse::newHttpJsonResponse(userRowToJson(*rows,0));
	r->setStatusCode(k200OK); cb(r);
}

/** """ Mise à jour (owner/admin) – ignore toute tentative de modifier admin côté non-admin """ */
void UserController::updateUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int userId) {
	if (!isLogged(req) || !canActOn(req, userId)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto body = req->getJsonObject();
	if (!body) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Body vide"}});
		r->setStatusCode(k400BadRequest); return cb(r);
	}

	std::string setSql;
	std::vector<std::string> sets;
	if ((*body).isMember("name")) sets.push_back("username='"+(*body)["name"].asString()+"'");
	if ((*body).isMember("email")) sets.push_back("email='"+(*body)["email"].asString()+"'");
	// Protection élévation : is_admin ignoré ici
	if (sets.empty()) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", false}});
		r->setStatusCode(k200OK); return cb(r);
	}
	for (size_t i=0;i<sets.size();i++) {
		setSql += sets[i];
		if (i+1<sets.size()) setSql += ", ";
	}
	auto db = app().getDbClient();
	db->execSqlSync("UPDATE users SET "+setSql+" WHERE id=$1", userId);
	auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	r->setStatusCode(k200OK); cb(r);
}

/** """ Suppression (owner/admin) """ */
void UserController::deleteUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int userId) {
	if (!isLogged(req) || !canActOn(req, userId)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM users WHERE id=$1", userId);
	auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	r->setStatusCode(k200OK); cb(r);
}

/** """ Liste admin: admins d'abord puis tri alphabétique par name """ */
void UserController::listAdminUsers(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	if (!isAdmin(req)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto db = app().getDbClient();
	auto rows = db->execSqlSync(
		"SELECT id,email,username AS name,is_admin AS admin FROM users "
		"ORDER BY admin DESC, name ASC"
	);
	Json::Value arr(Json::arrayValue);
	for (size_t i=0;i<rows->size();i++) arr.append(userRowToJson(*rows, i));
	auto r = HttpResponse::newHttpJsonResponse(arr);
	r->setStatusCode(k200OK); cb(r);
}

/** """ Mise à jour admin (peut modifier is_admin) """ */
void UserController::updateAdminUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int userId) {
	if (!isAdmin(req)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto body = req->getJsonObject();
	if (!body) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Body vide"}});
		r->setStatusCode(k400BadRequest); return cb(r);
	}

	std::vector<std::string> sets;
	if ((*body).isMember("name")) sets.push_back("username='"+(*body)["name"].asString()+"'");
	if ((*body).isMember("email")) sets.push_back("email='"+(*body)["email"].asString()+"'");
	if ((*body).isMember("admin")) sets.push_back(std::string("is_admin=")+((*body)["admin"].asBool()?"true":"false"));

	if (sets.empty()) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", false}});
		r->setStatusCode(k200OK); return cb(r);
	}
	std::string setSql;
	for (size_t i=0;i<sets.size();i++) {
		setSql += sets[i];
		if (i+1<sets.size()) setSql += ", ";
	}
	auto db = app().getDbClient();
	db->execSqlSync("UPDATE users SET "+setSql+" WHERE id=$1", userId);
	auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"updated", true}});
	r->setStatusCode(k200OK); cb(r);
}

/** """ Suppression admin """ */
void UserController::deleteAdminUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb, int userId) {
	if (!isAdmin(req)) {
		auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"error","Forbidden"}});
		r->setStatusCode(k403Forbidden); return cb(r);
	}
	auto db = app().getDbClient();
	db->execSqlSync("DELETE FROM users WHERE id=$1", userId);
	auto r = HttpResponse::newHttpJsonResponse(Json::Value{{"deleted", true}});
	r->setStatusCode(k200OK); cb(r);
}
