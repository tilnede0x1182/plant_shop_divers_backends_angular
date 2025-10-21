#include "AuthController.h"
#include <drogon/orm/Mapper.h>
#include <argon2.h>
#include <string>
#include <sstream>
#include <vector>
using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Users;

/** """ Hachage Argon2id """ */
static std::string hashPassword(const std::string& pwd) {
	const uint32_t t_cost = 2, m_cost = 1 << 16, parallel = 1;
	std::vector<uint8_t> salt(16);
	for (auto &b : salt) b = rand() % 256;
	std::vector<uint8_t> hash(32);
	if (argon2id_hash_raw(t_cost, m_cost, parallel, pwd.data(), pwd.size(),
		salt.data(), salt.size(), hash.data(), hash.size()) != ARGON2_OK)
		throw std::runtime_error("Argon2 hash failed");
	std::ostringstream oss;
	for (auto b : hash) oss << std::hex << (int)b;
	return oss.str();
}
static bool verifyPassword(const std::string& pwd, const std::string& hash) {
	return hashPassword(pwd) == hash;
}

/** Parsing JSON sûr */
static bool parseJson(const HttpRequestPtr& req, Json::Value& data) {
	try {
		std::string body(req->body());
		if (body.empty()) return false;
		Json::CharReaderBuilder b; std::string errs;
		std::unique_ptr<Json::CharReader> r(b.newCharReader());
		return r->parse(body.data(), body.data() + body.size(), &data, &errs);
	} catch (...) { return false; }
}

/** Inscription */
void AuthController::registerUser(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	Json::Value d; if (!parseJson(req, d) || !d.isMember("email") || !d.isMember("password")) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Champs manquants"}});
		r->setStatusCode(k400BadRequest); return cb(r);
	}
	try {
		Mapper<Users> users(app().getDbClient());
		Users u;
		u.setEmail(d["email"].asString());
		u.setUsername(d["username"].asString());
		u.setPasswordHash(hashPassword(d["password"].asString()));
		u.setIsAdmin(false);
		users.insert(u);
		auto r = HttpResponse::newHttpResponse();
		r->setStatusCode(k201Created); cb(r);
	} catch (const DrogonDbException&) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Email déjà utilisé"}});
		r->setStatusCode(k409Conflict); cb(r);
	}
}

/** Connexion */
void AuthController::login(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	Json::Value d; if (!parseJson(req, d) || !d.isMember("email") || !d.isMember("password")) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Champs manquants"}});
		r->setStatusCode(k400BadRequest); return cb(r);
	}
	try {
		Mapper<Users> users(app().getDbClient());
		auto userOpt = users.findOne(Criteria(Users::Cols::_email, d["email"].asString()));
		if (!userOpt) {
			auto r = HttpResponse::newHttpJsonResponse({{"error","Utilisateur introuvable"}});
			r->setStatusCode(k401Unauthorized); return cb(r);
		}
		auto user = *userOpt;
		if (!verifyPassword(d["password"].asString(), user.getValueOfPasswordHash())) {
			auto r = HttpResponse::newHttpJsonResponse({{"error","Mot de passe invalide"}});
			r->setStatusCode(k401Unauthorized); return cb(r);
		}
		Json::Value j;
		j["id"] = user.getValueOfId();
		j["username"] = user.getValueOfUsername();
		j["email"] = user.getValueOfEmail();
		j["is_admin"] = user.getValueOfIsAdmin();
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->addCookie("auth_user", user.getValueOfEmail());
		r->setStatusCode(k201Created);
		cb(r);
	} catch (...) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Erreur serveur"}});
		r->setStatusCode(k500InternalServerError); cb(r);
	}
}

/** Profil /auth/me */
void AuthController::me(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	auto ck = req->cookies();
	if (!ck.count("auth_user")) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Non connecté"}});
		r->setStatusCode(k401Unauthorized); return cb(r);
	}
	try {
		Mapper<Users> users(app().getDbClient());
		auto u = users.findOne(Criteria(Users::Cols::_email, ck.at("auth_user")));
		if (!u) {
			auto r = HttpResponse::newHttpJsonResponse({{"error","Utilisateur inconnu"}});
			r->setStatusCode(k404NotFound); return cb(r);
		}
		Json::Value j;
		j["id"] = u->getValueOfId();
		j["email"] = u->getValueOfEmail();
		j["username"] = u->getValueOfUsername();
		j["is_admin"] = u->getValueOfIsAdmin();
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k200OK);
		cb(r);
	} catch (...) {
		auto r = HttpResponse::newHttpJsonResponse({{"error","Erreur serveur"}});
		r->setStatusCode(k500InternalServerError); cb(r);
	}
}

/** Déconnexion */
void AuthController::logout(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
	auto r = HttpResponse::newHttpJsonResponse({{"success",true}});
	Cookie c("auth_user",""); c.setPath("/"); c.setExpiresDate(trantor::Date(0));
	r->addCookie(std::move(c)); r->setStatusCode(k200OK); cb(r);
}
