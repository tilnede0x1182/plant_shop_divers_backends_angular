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
#include "../models/Users.h"
#include "../utils/TokenUtils.h"

using namespace drogon_model::plant_shop_cpp;
struct SessionData {
    std::string email;
    bool isAdmin;
    int64_t userId;
    std::string name;
};

// Store et mutex global
static std::unordered_map<std::string, SessionData> sessionStore;
static std::mutex sessionMutex;

using namespace drogon;
using namespace drogon::orm;
using drogon_model::plant_shop_cpp::Users;

/** """ Hachage Argon2id """ */
static std::string hashPassword(const std::string& pwd) {
	const uint32_t t_cost = 2, m_cost = 1 << 16, parallel = 1;
	std::vector<uint8_t> salt(16);
	for (auto &b : salt) b = rand() % 256;

	// Taille max pour le hash encodé (≈108 caractères)
	char encoded[128];
	int result = argon2id_hash_encoded(
		t_cost, m_cost, parallel,
		pwd.data(), pwd.size(),
		salt.data(), salt.size(),
		32, encoded, sizeof(encoded)
	);

	if (result != ARGON2_OK)
		throw std::runtime_error("Argon2id hash_encoded failed");

	return std::string(encoded);
}

/** """ Vérifie un mot de passe contre un hash Argon2id """ */
/**
 * Vérifie un mot de passe contre un hash Argon2id.
 *
 * @param pwd Mot de passe en clair
 * @param encodedHash Hash encodé Argon2id
 * @return true si le mot de passe correspond
 */
static bool verifyPassword(const std::string& pwd, const std::string& encodedHash) {
	int result = argon2id_verify(encodedHash.c_str(), pwd.data(), pwd.size());
	return result == ARGON2_OK;
}

/**
 * Parse le corps JSON d une requête HTTP.
 *
 * @param req Requête HTTP
 * @param data Objet JSON de sortie
 * @return true si parsing réussi
 */
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
		u.setUsername(d.isMember("name") ? d["name"].asString() : d["username"].asString());
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

/* ---- Helpers génériques ---- */
static HttpResponsePtr err(int code, const std::string &msg) {
	Json::Value j;
	j["error"] = msg;
	auto r = HttpResponse::newHttpJsonResponse(j);
	r->setStatusCode((HttpStatusCode)code);
	return r;
}

/** """ Vérifie si le cookie jwt indique un admin @req requête HTTP """ */
bool AuthController::isAdmin(const drogon::HttpRequestPtr& req) {
    const auto& cookies = req->cookies();
    LOG_DEBUG << "[isAdmin] cookies count: " << cookies.size();

    // 1. Lire jwt direct
    auto it = cookies.find("jwt");
    if (it != cookies.end()) {
        LOG_DEBUG << "[isAdmin] jwt cookie found.";
        std::string email; std::string name; int64_t userId = 0; bool admin = false;
        if (parseToken(it->second, email, userId, name, admin)) return admin;
    }

    // 2. Sinon tenter via JSESSIONID → sessionStore
    auto s = cookies.find("JSESSIONID");
    if (s != cookies.end()) {
        std::lock_guard<std::mutex> lock(sessionMutex);
        auto found = sessionStore.find(s->second);
        if (found != sessionStore.end()) {
            LOG_DEBUG << "[isAdmin] resolved from JSESSIONID, email=" << found->second.email;
            return found->second.isAdmin;
        }
    }

    LOG_DEBUG << "[isAdmin] no valid session found";
    return false;
}

/** """ Vérifie si la requête peut agir : admin OU propriétaire """ */
bool AuthController::canAct(const HttpRequestPtr& req, int uid) {
	auto userOpt = AuthController::canActDecodeJWT(req);	// JWT ou JSESSIONID
	if (!userOpt.has_value()) return false;
	const auto& user = userOpt.value();
	return user.getValueOfIsAdmin() || user.getValueOfId() == uid;
}

/** """ Décode le JWT ou résout via JSESSIONID pour récupérer l'utilisateur correspondant """ */
std::optional<drogon_model::plant_shop_cpp::Users> AuthController::canActDecodeJWT(const HttpRequestPtr &req) {
	std::string email; std::string name; int64_t userId = 0; bool admin = false;

	// 1. Essayer le cookie JWT
	auto it = req->cookies().find("jwt");
	if (it != req->cookies().end() && parseToken(it->second, email, userId, name, admin)) {
		try {
			Mapper<drogon_model::plant_shop_cpp::Users> users(app().getDbClient());
			auto user = users.findByPrimaryKey(static_cast<int>(userId));
			return user;
		} catch (...) {}
	}

	// 2. Sinon tenter via JSESSIONID → sessionStore
	auto s = req->cookies().find("JSESSIONID");
	if (s != req->cookies().end()) {
		std::lock_guard<std::mutex> lock(sessionMutex);
		auto found = sessionStore.find(s->second);
		if (found != sessionStore.end()) {
			try {
				Mapper<drogon_model::plant_shop_cpp::Users> users(app().getDbClient());
				auto user = users.findOne(Criteria(
					drogon_model::plant_shop_cpp::Users::Cols::_email,
					CompareOperator::EQ,
					found->second.email
				));
				return user;
			} catch (...) {}
		}
	}

	return std::nullopt;
}

/** """ Version booléenne de canActDecodeJWT : admin ou propriétaire """ */
bool AuthController::canActDecodeJWTBool(const HttpRequestPtr &req, int uid) {
	auto userOpt = AuthController::canActDecodeJWT(req);
	if (!userOpt.has_value()) return false;
	auto user = userOpt.value();
	return user.getValueOfIsAdmin() || user.getValueOfId() == uid;
}

/** Connexion utilisateur */
void AuthController::login(const drogon::HttpRequestPtr& req,
	std::function<void (const drogon::HttpResponsePtr&)>&& cb) {
	try {
		auto json = req->getJsonObject();
		if (!json) throw std::runtime_error("payload");

		auto email = (*json)["email"].asString();
		auto password = (*json)["password"].asString();

		drogon::orm::Mapper<Users> users(drogon::app().getDbClient());
		auto user = users.findOne(
			drogon::orm::Criteria(Users::Cols::_email, drogon::orm::CompareOperator::EQ, email)
		);

		if (!verifyPassword(password, user.getValueOfPasswordHash()))
			throw std::runtime_error("unauthorized");

		const bool admin = user.getValueOfIsAdmin();
		const int64_t userId = user.getValueOfId();
		const std::string name = user.getValueOfUsername();
		const std::string token = generateToken(email, userId, name, admin);
		std::string sessionId = drogon::utils::getUuid();
		{
				std::lock_guard<std::mutex> lock(sessionMutex);
				sessionStore[sessionId] = { email, admin, userId, name };
		}
		// Crée la réponse HTTP vide d’abord
		auto resp = drogon::HttpResponse::newHttpResponse();
		// Ajoute le cookie AVANT tout corps JSON
		drogon::Cookie c("jwt", token);
		c.setHttpOnly(true);
		c.setPath("/");
		c.setSameSite(drogon::Cookie::SameSite::kLax);
		c.setSecure(false);
		c.setMaxAge(7 * 24 * 3600);
		resp->addCookie(c);
		drogon::Cookie sessionCookie("JSESSIONID", sessionId);
		sessionCookie.setPath("/");
		resp->addCookie(sessionCookie);
		resp->addHeader("Set-Cookie", "JSESSIONID=" + sessionId + "; Path=/; HttpOnly");

		// Corps JSON ensuite
		Json::Value body(Json::objectValue);
		body["status"] = "ok";
		resp->setBody(body.toStyledString());
		resp->setContentTypeCode(drogon::CT_APPLICATION_JSON);
		resp->setStatusCode(drogon::k201Created);
		resp->addHeader("Access-Control-Allow-Origin", "*");
		resp->addHeader("Access-Control-Allow-Credentials", "true");
		auto age = c.maxAge().has_value() ? std::to_string(*c.maxAge()) : "none";
		LOG_DEBUG << "[login] token generated: " << token;
		LOG_DEBUG << "[login] cookie params - HttpOnly=" << c.isHttpOnly()
							<< " SameSite=" << static_cast<int>(c.sameSite())
							<< " Secure=" << c.isSecure()
							<< " MaxAge=" << age;
		cb(std::move(resp));
	}
	catch (...) {
		LOG_DEBUG << "[login] exception thrown, unauthorized";
		auto r = drogon::HttpResponse::newHttpResponse();
		r->setStatusCode(drogon::k401Unauthorized);
		cb(std::move(r));
	}
}

/** Profil /auth/me */
void AuthController::me(const drogon::HttpRequestPtr& req,
	std::function<void (const drogon::HttpResponsePtr&)>&& cb) {
	std::string email; std::string name; int64_t userId = 0; bool admin = false;
	auto it = req->cookies().find("jwt");
	bool ok = it != req->cookies().end() && parseToken(it->second, email, userId, name, admin);

	// Si JWT invalide, essayer JSESSIONID
	if (!ok) {
		auto s = req->cookies().find("JSESSIONID");
		if (s != req->cookies().end()) {
			std::lock_guard<std::mutex> lock(sessionMutex);
			auto found = sessionStore.find(s->second);
			if (found != sessionStore.end()) {
				email  = found->second.email;
				admin  = found->second.isAdmin;
				userId = found->second.userId;
				name   = found->second.name;
				ok = true;
			}
		}
	}

	if (!ok) {
		auto r = drogon::HttpResponse::newHttpResponse();
		r->setStatusCode(drogon::k401Unauthorized);
		cb(r);
		return;
	}

	Json::Value j(Json::objectValue);
	j["id"]    = Json::Int64(userId);
	j["email"] = email;
	j["name"]  = name;
	j["admin"] = admin;
	cb(drogon::HttpResponse::newHttpJsonResponse(j));
}

/** Déconnexion */
void AuthController::logout(const drogon::HttpRequestPtr& req,
	std::function<void (const drogon::HttpResponsePtr&)>&& cb) {
	auto resp = drogon::HttpResponse::newHttpJsonResponse(Json::Value(Json::objectValue));
	drogon::Cookie c("jwt", ""); c.setHttpOnly(true); c.setPath("/"); c.setSameSite(drogon::Cookie::SameSite::kLax); c.setMaxAge(0);
	resp->addCookie(c); resp->setStatusCode(drogon::k200OK); cb(resp);
}
