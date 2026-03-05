#pragma once
#include <drogon/drogon.h>
#include <json/json.h>

/**
 * Controleur des utilisateurs.
 * Routes gerees : CRUD /api/users et /api/admin/users.
 */
class UserController : public drogon::HttpController<UserController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(UserController::createUser, "/api/users", drogon::Post);
		ADD_METHOD_TO(UserController::listUsers, "/api/users", drogon::Get);
		ADD_METHOD_TO(UserController::getUser, "/api/users/{1}", drogon::Get);
		ADD_METHOD_TO(UserController::updateUser, "/api/users/{1}", drogon::Patch);
		ADD_METHOD_TO(UserController::deleteUser, "/api/users/{1}", drogon::Delete);

		ADD_METHOD_TO(UserController::listAdminUsers, "/api/admin/users", drogon::Get);
		ADD_METHOD_TO(UserController::updateAdminUser, "/api/admin/users/{1}", drogon::Patch);
		ADD_METHOD_TO(UserController::deleteAdminUser, "/api/admin/users/{1}", drogon::Delete);
	METHOD_LIST_END

	/**
 * Creation d un utilisateur (admin).
 *
 * @param req Requete HTTP avec JSON
 * @param cb Callback de reponse
 */
	void createUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/**
 * Liste tous les utilisateurs (admin).
 *
 * @param req Requete HTTP
 * @param cb Callback de reponse
 */
	void listUsers(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/**
 * Recupere un utilisateur (owner ou admin).
 *
 * @param req Requete HTTP
 * @param cb Callback de reponse
 * @param userId Identifiant utilisateur
 */
	void getUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/**
 * Met a jour un utilisateur (owner ou admin).
 *
 * @param req Requete HTTP avec JSON
 * @param cb Callback de reponse
 * @param userId Identifiant utilisateur
 */
	void updateUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/**
 * Supprime un utilisateur (owner ou admin).
 *
 * @param req Requete HTTP
 * @param cb Callback de reponse
 * @param userId Identifiant utilisateur
 */
	void deleteUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/**
 * Liste admin: tri admin d abord puis name/email.
 *
 * @param req Requete HTTP
 * @param cb Callback de reponse
 */
	void listAdminUsers(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/**
 * Mise a jour admin (peut modifier is_admin).
 *
 * @param req Requete HTTP avec JSON
 * @param cb Callback de reponse
 * @param userId Identifiant utilisateur
 */
	void updateAdminUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/**
 * Suppression admin.
 *
 * @param req Requete HTTP
 * @param cb Callback de reponse
 * @param userId Identifiant utilisateur
 */
	void deleteAdminUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);
};
