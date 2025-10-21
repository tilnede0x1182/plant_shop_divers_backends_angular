#include "AuthController.h"
#include <drogon/orm/Mapper.h>
#include <argon2.h>
#include <string>
#include <sstream>
#include <vector>

#include "../models/Users.h"
#include "../models/Plants.h"
#include "../models/Orders.h"
#include "../models/OrderItems.h"
using namespace drogon_model::plant_shop_cpp;

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
	Json::Value d;
	if (!parseJson(req, d) || !d.isMember("email") || !d.isMember("password")) {
		Json::Value j; j["error"] = "Champs manquants";
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k400BadRequest);
		return cb(r);
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
		r->setStatusCode(k201Created);
		cb(r);
	} catch (const DrogonDbException&) {
		Json::Value j; j["error"] = "Email déjà utilisé";
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k409Conflict);
		cb(r);
	}
}

/** Connexion utilisateur */
void AuthController::login(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& callback) {

	try {
		// Lecture du corps JSON avec logs détaillés
		LOG_INFO << "🔹 [login] Début /auth/login";
		LOG_INFO << "🔹 [login] Headers=" << req->headers().size()
						<< " | Body length=" << req->getBody().length();
		LOG_INFO << "🔹 [login] Content-Type=" << req->getHeader("content-type");

		Json::Value data;
		bool ok = parseJson(req, data);
		LOG_INFO << "🔹 [login] Résultat parseJson=" << (ok ? "true" : "false");

		if (!ok) {
			LOG_ERROR << "❌ [login] JSON invalide ou vide";
			Json::Value j; j["error"] = "JSON invalide";
			auto r = HttpResponse::newHttpJsonResponse(j);
			r->setStatusCode(k400BadRequest);
			return callback(r);
		}

		LOG_INFO << "🔹 [login] JSON brut reçu: " << data.toStyledString();

		if (!data.isMember("email") || !data.isMember("password")) {
			LOG_ERROR << "❌ [login] Champs email/password manquants";
			Json::Value j; j["error"] = "Champs manquants";
			auto r = HttpResponse::newHttpJsonResponse(j);
			r->setStatusCode(k400BadRequest);
			return callback(r);
		}

		std::string email = data["email"].asString();
		std::string password = data["password"].asString();

		LOG_INFO << "🔹 [login] email=" << email
						<< " | password length=" << password.size();
		LOG_INFO << "🔹 [login] Fin parsing JSON, préparation DB...";

		auto db = app().getDbClient("default");
		if (!db) {
			LOG_FATAL << "💥 Aucun client DB 'default' disponible";
			throw std::runtime_error("DB client 'default' manquant");
		}
		Mapper<Users> users(db);

		Users user;
		try {
			user = users.findOne(Criteria(Users::Cols::_email, email));
			LOG_DEBUG << "Utilisateur trouvé, id=" << user.getValueOfId();
		} catch (const DrogonDbException &e) {
			LOG_ERROR << "Utilisateur introuvable: " << e.base().what();
			Json::Value j; j["error"] = "Utilisateur introuvable";
			auto r = HttpResponse::newHttpJsonResponse(j);
			r->setStatusCode(k401Unauthorized);
			return callback(r);
		}

		if (!verifyPassword(password, user.getValueOfPasswordHash())) {
			LOG_WARN << "Mot de passe invalide pour " << email;
			Json::Value j; j["error"] = "Mot de passe invalide";
			auto r = HttpResponse::newHttpJsonResponse(j);
			r->setStatusCode(k401Unauthorized);
			return callback(r);
		}

		Json::Value j;
		j["id"] = user.getValueOfId();
		j["username"] = user.getValueOfUsername();
		j["email"] = user.getValueOfEmail();
		j["is_admin"] = user.getValueOfIsAdmin();

		auto r = HttpResponse::newHttpJsonResponse(j);
		r->addCookie("auth_user", user.getValueOfEmail());
		r->setStatusCode(k201Created);
		callback(r);

	} catch (const std::exception &e) {
		LOG_FATAL << "💥 Exception dans /auth/login: " << e.what();
		Json::Value j; j["error"] = "Erreur serveur";
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k500InternalServerError);
		callback(r);
	}
}

/** Profil /auth/me */
void AuthController::me(const HttpRequestPtr& req,
	std::function<void(const HttpResponsePtr&)>&& cb) {
	auto ck = req->cookies();
	if (!ck.count("auth_user")) {
		Json::Value j; j["error"] = "Non connecté";
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k401Unauthorized);
		return cb(r);
	}
	try {
		Mapper<Users> users(app().getDbClient());
		auto u = users.findOne(Criteria(Users::Cols::_email, ck.at("auth_user")));
		Users usr;
		try {
			usr = users.findOne(Criteria(Users::Cols::_email, ck.at("auth_user")));
		} catch (const DrogonDbException &e) {
			Json::Value j;
			j["error"] = "Utilisateur inconnu";
			auto r = HttpResponse::newHttpJsonResponse(j);
			r->setStatusCode(k404NotFound);
			return cb(r);
		}
		Json::Value j;
		j["id"] = usr.getValueOfId();
		j["email"] = usr.getValueOfEmail();
		j["username"] = usr.getValueOfUsername();
		j["is_admin"] = usr.getValueOfIsAdmin();
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k200OK);
		cb(r);
	} catch (...) {
		Json::Value j; j["error"] = "Erreur serveur";
		auto r = HttpResponse::newHttpJsonResponse(j);
		r->setStatusCode(k500InternalServerError);
		cb(r);
	}
}

/** Déconnexion */
void AuthController::logout(const HttpRequestPtr&, std::function<void(const HttpResponsePtr&)>&& cb) {
	Json::Value j; j["success"] = true;
	auto r = HttpResponse::newHttpJsonResponse(j);
	Cookie c("auth_user", "");
	c.setPath("/");
	c.setExpiresDate(trantor::Date(0));
	r->addCookie(std::move(c));
	r->setStatusCode(k200OK);
	cb(r);
}
