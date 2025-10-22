#pragma once
#include <drogon/drogon.h>
#include <json/json.h>
#include "../models/Users.h"
#include <optional>

/**
 * """ Contrôleur d'authentification
 *  Gère les routes :
 *   - POST /api/auth/register
 *   - POST /api/auth/login
 *   - GET  /api/auth/me
 *   - POST /api/auth/logout
  *  Fournit aussi :
 *   - Méthode statique isAdmin(req) pour vérifier les droits administrateur
 * """
 */
class AuthController : public drogon::HttpController<AuthController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(AuthController::registerUser, "/api/auth/register", drogon::Post);
		ADD_METHOD_TO(AuthController::login, "/api/auth/login", drogon::Post);
		ADD_METHOD_TO(AuthController::me, "/api/auth/me", drogon::Get);
		ADD_METHOD_TO(AuthController::logout, "/api/auth/logout", drogon::Post);
	METHOD_LIST_END

	/** """ Vérifie si l'utilisateur est administrateur """ */
	static bool isAdmin(const drogon::HttpRequestPtr& req);

	/** """ Vérifie si la requête provient d’un admin ou du propriétaire """ */
	static bool canAct(const drogon::HttpRequestPtr &req, int uid);

	/** """ Décode le JWT pour récupérer l'id, puis appelle canAct(req, id) """ */
	static std::optional<drogon_model::plant_shop_cpp::Users> canActDecodeJWT(const drogon::HttpRequestPtr &req);

	/** """ Inscription d'un utilisateur """ */
	void registerUser(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Connexion d'un utilisateur """ */
	void login(const drogon::HttpRequestPtr& req,
	           std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Retourne le profil de l'utilisateur connecté """ */
	void me(const drogon::HttpRequestPtr& req,
	        std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Déconnexion utilisateur (invalide le cookie) """ */
	void logout(const drogon::HttpRequestPtr& req,
	            std::function<void(const drogon::HttpResponsePtr&)>&& callback);
};
