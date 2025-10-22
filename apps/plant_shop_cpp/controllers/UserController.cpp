#include "UserController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
#include "AuthController.h"

#include "../models/Users.h"
#include "../models/Plants.h"
#include "../models/Orders.h"
#include "../models/OrderItems.h"
using namespace drogon_model::plant_shop_cpp;

using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Users;

/* ---- Helpers génériques ---- */
static HttpResponsePtr err(int code, const std::string &msg) {
	Json::Value j;
	j["error"] = msg;
	auto r = HttpResponse::newHttpJsonResponse(j);
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

static std::optional<Users> currentUser(const HttpRequestPtr &req) {
	try {
		if (!req->cookies().count("auth_user")) return std::nullopt;
		Mapper<Users> mu(app().getDbClient());
		auto u = mu.findOne(Criteria(Users::Cols::_email, req->cookies().at("auth_user")));
		return u;
	} catch (...) { return std::nullopt; }
}

static bool canAct(const HttpRequestPtr &req, int uid) {
	auto u = currentUser(req);
	return u.has_value() && (u->getValueOfId() == uid || u->getValueOfIsAdmin());
}

/* ---- Création (admin) ---- */
void UserController::createUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb) {
	if (!AuthController::isAdmin(req)) return cb(err(403, "Forbidden"));
	auto j = req->getJsonObject();
	if (!j || !j->isMember("email") || !j->isMember("password"))
		return cb(err(400, "Champs manquants"));
	try {
		Mapper<Users> m(app().getDbClient());
		Users u;
		u.setEmail((*j)["email"].asString());
		u.setUsername(j->isMember("name") ? (*j)["name"].asString() : (*j)["username"].asString());
		u.setPasswordHash(utils::getMd5((*j)["password"].asString()));
		u.setIsAdmin(j->isMember("admin") ? (*j)["admin"].asBool() : false);
		m.insert(u);
		Json::Value resp;
		resp["id"] = u.getValueOfId();
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(409, "Email déjà utilisé")); }
}

/* ---- Liste utilisateurs (admin) ---- */
void UserController::listUsers(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb) {
	if (!AuthController::isAdmin(req)) return cb(err(403, "Forbidden"));
	try {
		Mapper<Users> m(app().getDbClient());
		auto all = m.findAll();
		Json::Value arr(Json::arrayValue);
		for (auto &u : all) {
			Json::Value j;
			j["id"] = u.getValueOfId();
			j["email"] = u.getValueOfEmail();
			j["name"] = u.getValueOfUsername();
			j["admin"] = u.getValueOfIsAdmin();
			arr.append(j);
		}
		cb(HttpResponse::newHttpJsonResponse(arr));
	} catch (...) { cb(err(500, "Erreur serveur")); }
}

/* ---- Lecture ---- */
void UserController::getUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb, int id) {
	if (!canAct(req, id)) return cb(err(403, "Forbidden"));
	try {
		Mapper<Users> m(app().getDbClient());
		auto u = m.findByPrimaryKey(id);
		Json::Value j;
		j["id"] = u.getValueOfId();
		j["email"] = u.getValueOfEmail();
		j["name"] = u.getValueOfUsername();
		j["admin"] = u.getValueOfIsAdmin();
		cb(HttpResponse::newHttpJsonResponse(j));
	} catch (...) { cb(err(404, "Not found")); }
}

/* ---- Mise à jour ---- */
void UserController::updateUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb, int id) {
	if (!canAct(req, id)) return cb(err(403, "Forbidden"));
	auto j = req->getJsonObject();
	if (!j) return cb(err(400, "Body vide"));
	try {
		Mapper<Users> m(app().getDbClient());
		auto u = m.findByPrimaryKey(id);
		if (j->isMember("name")) u.setUsername((*j)["name"].asString());
		if (j->isMember("email")) u.setEmail((*j)["email"].asString());
		m.update(u);
		Json::Value resp;
		resp["updated"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404, "User introuvable")); }
}

/* ---- Suppression ---- */
void UserController::deleteUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb, int id) {
	if (!canAct(req, id)) return cb(err(403, "Forbidden"));
	try {
		Mapper<Users> m(app().getDbClient());
		m.deleteByPrimaryKey(id);
		Json::Value resp;
		resp["deleted"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404, "User introuvable")); }
}

/* ---- Liste admins ---- */
void UserController::listAdminUsers(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb) {
	if (!AuthController::isAdmin(req)) return cb(err(403, "Forbidden"));
	try {
		Mapper<Users> m(app().getDbClient());
		auto all = m.findAll();
		std::sort(all.begin(), all.end(), [](const Users &a, const Users &b) {
			if (a.getValueOfIsAdmin() != b.getValueOfIsAdmin())
				return a.getValueOfIsAdmin() > b.getValueOfIsAdmin();
			return a.getValueOfUsername() < b.getValueOfUsername();
		});
		Json::Value arr(Json::arrayValue);
		for (auto &u : all) {
			Json::Value j;
			j["id"] = u.getValueOfId();
			j["email"] = u.getValueOfEmail();
			j["name"] = u.getValueOfUsername();
			j["admin"] = u.getValueOfIsAdmin();
			arr.append(j);
		}
		cb(HttpResponse::newHttpJsonResponse(arr));
	} catch (...) { cb(err(500, "Erreur serveur")); }
}

/* ---- Mise à jour admin ---- */
void UserController::updateAdminUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb, int id) {
	if (!AuthController::isAdmin(req)) return cb(err(403, "Forbidden"));
	auto j = req->getJsonObject();
	if (!j) return cb(err(400, "Body vide"));
	try {
		Mapper<Users> m(app().getDbClient());
		auto u = m.findByPrimaryKey(id);
		if (j->isMember("name")) u.setUsername((*j)["name"].asString());
		if (j->isMember("email")) u.setEmail((*j)["email"].asString());
		if (j->isMember("admin")) u.setIsAdmin((*j)["admin"].asBool());
		m.update(u);
		Json::Value resp;
		resp["updated"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404, "User introuvable")); }
}

/* ---- Suppression admin ---- */
void UserController::deleteAdminUser(const HttpRequestPtr &req, std::function<void(const HttpResponsePtr &)> &&cb, int id) {
	if (!AuthController::isAdmin(req)) return cb(err(403, "Forbidden"));
	try {
		Mapper<Users> m(app().getDbClient());
		m.deleteByPrimaryKey(id);
		Json::Value resp;
		resp["deleted"] = true;
		cb(HttpResponse::newHttpJsonResponse(resp));
	} catch (...) { cb(err(404, "User introuvable")); }
}
