#pragma once
#include <drogon/drogon.h>
#include <json/json.h>

/**
 * """ Contrôleur utilisateurs
 *  Routes:
 *   - POST   /api/users
 *   - GET    /api/users
 *   - GET    /api/users/{id}
 *   - PATCH  /api/users/{id}
 *   - DELETE /api/users/{id}
 *   - GET    /api/admin/users
 *   - PATCH  /api/admin/users/{id}
 *   - DELETE /api/admin/users/{id}
 * """
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

	/** """ Création d'un utilisateur (admin) """ */
	void createUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/** """ Liste tous les utilisateurs (admin) """ */
	void listUsers(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/** """ Récupère un utilisateur (owner ou admin) """ */
	void getUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/** """ Met à jour un utilisateur (owner ou admin) """ */
	void updateUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/** """ Supprime un utilisateur (owner ou admin) """ */
	void deleteUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/** """ Liste admin: tri admin d'abord puis name/email """ */
	void listAdminUsers(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb);

	/** """ Mise à jour admin (peut modifier is_admin) """ */
	void updateAdminUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);

	/** """ Suppression admin """ */
	void deleteAdminUser(const drogon::HttpRequestPtr& req,
		std::function<void(const drogon::HttpResponsePtr&)>&& cb, int userId);
};
