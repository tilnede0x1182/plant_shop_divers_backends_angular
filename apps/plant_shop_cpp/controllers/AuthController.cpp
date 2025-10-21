#include "AuthController.h"
#include <drogon/orm/Mapper.h>
#include <drogon/utils/Utilities.h>
#include <argon2.h>
#include <vector>
#include <string>
#include <sstream>
#include <stdexcept>
#include <cstdlib>
using namespace drogon;
using namespace drogon::orm;

/** """ Fonctions de hachage et vérification Argon2id
	@password mot de passe clair """ */
static std::string hashPassword(const std::string& password) {
	const uint32_t t_cost = 2;        // nombre d’itérations
	const uint32_t m_cost = 1 << 16;  // mémoire utilisée (~64 MiB)
	const uint32_t parallelism = 1;
	const size_t hashLen = 32;
	const size_t saltLen = 16;

	std::vector<uint8_t> salt(saltLen, 0);
	for (size_t i = 0; i < saltLen; ++i)
		salt[i] = static_cast<uint8_t>(rand() % 256);

	std::vector<uint8_t> hash(hashLen);
	int res = argon2id_hash_raw(
		t_cost, m_cost, parallelism,
		password.data(), password.size(),
		salt.data(), salt.size(),
		hash.data(), hash.size()
	);

	if (res != ARGON2_OK)
		throw std::runtime_error("Argon2 hashing failed");

	std::ostringstream oss;
	for (auto b : hash) oss << std::hex << (int)b;
	return oss.str();
}

static bool verifyPassword(const std::string& password, const std::string& hashHex) {
	return hashPassword(password) == hashHex;
}

/**
 * """ Inscription d’un nouvel utilisateur """
 */
void AuthController::registerUser(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("email") || !json->isMember("password") || !json->isMember("name")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Champs manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}

	std::string email = (*json)["email"].asString();
	std::string username = (*json)["name"].asString();
	std::string password = hashPassword((*json)["password"].asString());

	try {
		auto client = app().getDbClient();
		client->execSqlSync(
			"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,false)",
			email, username, password
		);
		auto resp = HttpResponse::newHttpResponse();
		resp->setStatusCode(k201Created);
		callback(resp);
	} catch (const DrogonDbException& e) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Email déjà utilisé"}});
		resp->setStatusCode(k409Conflict);
		callback(resp);
	}
}

/**
 * """ Connexion utilisateur (stocke cookie auth) """
 */
void AuthController::login(const HttpRequestPtr& req,
                           std::function<void(const HttpResponsePtr&)>&& callback) {
	auto json = req->getJsonObject();
	if (!json || !json->isMember("email") || !json->isMember("password")) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Champs manquants"}});
		resp->setStatusCode(k400BadRequest);
		return callback(resp);
	}

	std::string email = (*json)["email"].asString();
	std::string plain = (*json)["password"].asString();

	auto client = app().getDbClient();
	auto r = client->execSqlSync("SELECT id, username, password_hash, is_admin FROM users WHERE email=$1", email);

	if (r.size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Utilisateur introuvable"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}

	std::string hash = r[0]["password_hash"].as<std::string>();
	if (!verifyPassword(plain, hash)) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Mot de passe invalide"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}

	Json::Value u;
	u["id"] = r[0]["id"].as<int>();
	u["username"] = r[0]["username"].as<std::string>();
	u["email"] = email;
	u["is_admin"] = r[0]["is_admin"].as<bool>();

	auto resp = HttpResponse::newHttpJsonResponse(u);
	resp->addCookie("auth_user", email);
	resp->setStatusCode(k201Created);
	callback(resp);
}

/**
 * """ Récupération du profil /auth/me """
 */
void AuthController::me(const HttpRequestPtr& req,
                        std::function<void(const HttpResponsePtr&)>&& callback) {
	auto cookies = req->cookies();
	if (cookies.find("auth_user") == cookies.end()) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Non connecté"}});
		resp->setStatusCode(k401Unauthorized);
		return callback(resp);
	}

	std::string email = cookies.at("auth_user");
	auto client = app().getDbClient();
	auto r = client->execSqlSync("SELECT id, email, username, is_admin FROM users WHERE email=$1", email);

	if (r.size() == 0) {
		auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"error", "Utilisateur inconnu"}});
		resp->setStatusCode(k404NotFound);
		return callback(resp);
	}

	Json::Value j;
	j["id"] = r[0]["id"].as<int>();
	j["email"] = r[0]["email"].as<std::string>();
	j["username"] = r[0]["username"].as<std::string>();
	j["is_admin"] = r[0]["is_admin"].as<bool>();

	auto resp = HttpResponse::newHttpJsonResponse(j);
	resp->setStatusCode(k200OK);
	callback(resp);
}

/**
 * """ Déconnexion : suppression du cookie """
 */
void AuthController::logout(const HttpRequestPtr&,
                            std::function<void(const HttpResponsePtr&)>&& callback) {
	auto resp = HttpResponse::newHttpJsonResponse(Json::Value{{"success", true}});
	{
		drogon::Cookie authCookie("auth_user", "");
		authCookie.setPath("/");
		authCookie.setExpiresDate(trantor::Date(0)); // expiration au 1/1/1970
		resp->addCookie(std::move(authCookie));
	}
	resp->setStatusCode(k200OK);
	callback(resp);
}
