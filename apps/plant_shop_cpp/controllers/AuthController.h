#pragma once
#include <drogon/drogon.h>
#include <json/json.h>
#include "../models/Users.h"
#include <optional>

/**
 * Controleur d authentification.
 * Routes gerees : register, login, me, logout.
 */
class AuthController : public drogon::HttpController<AuthController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(AuthController::registerUser, "/api/auth/register", drogon::Post);
		ADD_METHOD_TO(AuthController::login, "/api/auth/login", drogon::Post);
		ADD_METHOD_TO(AuthController::me, "/api/auth/me", drogon::Get);
		ADD_METHOD_TO(AuthController::logout, "/api/auth/logout", drogon::Post);
	METHOD_LIST_END

	/**
	 * Verifie si l utilisateur est administrateur.
	 *
	 * @param req Requete HTTP
	 * @return true si admin
	 */
	static bool isAdmin(const drogon::HttpRequestPtr& req);

	/**
	 * Verifie si la requete provient d un admin ou du proprietaire.
	 *
	 * @param req Requete HTTP
	 * @param uid ID utilisateur cible
	 * @return true si autorise
	 */
	static bool canAct(const drogon::HttpRequestPtr &req, int uid);

	/**
	 * Decode le JWT pour recuperer l utilisateur.
	 *
	 * @param req Requete HTTP
	 * @return Utilisateur ou nullopt
	 */
	static std::optional<drogon_model::plant_shop_cpp::Users> canActDecodeJWT(const drogon::HttpRequestPtr &req);

	/**
	 * Version booleenne de canActDecodeJWT.
	 *
	 * @param req Requete HTTP
	 * @param uid ID utilisateur cible
	 * @return true si autorise
	 */
	static bool canActDecodeJWTBool(const drogon::HttpRequestPtr &req, int uid);

	/**
	 * Inscription d un utilisateur.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void registerUser(const drogon::HttpRequestPtr& req,
	                  std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
	 * Connexion d un utilisateur.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void login(const drogon::HttpRequestPtr& req,
	           std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
	 * Retourne le profil de l utilisateur connecte.
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void me(const drogon::HttpRequestPtr& req,
	        std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
	 * Deconnexion utilisateur (invalide le cookie).
	 *
	 * @param req Requete HTTP
	 * @param callback Callback de reponse
	 */
	void logout(const drogon::HttpRequestPtr& req,
	            std::function<void(const drogon::HttpResponsePtr&)>&& callback);
};
