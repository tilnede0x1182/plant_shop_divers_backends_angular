#pragma once
#include <drogon/drogon.h>
#include <json/json.h>


/**
 * Controleur des plantes.
 * Routes gerees : GET/POST/PATCH/DELETE /api/plants et /api/admin/plants.
 */
class PlantController : public drogon::HttpController<PlantController> {
public:
	METHOD_LIST_BEGIN
		ADD_METHOD_TO(PlantController::listPlants, "/api/plants", drogon::Get);
		ADD_METHOD_TO(PlantController::getPlant, "/api/plants/{1}", drogon::Get);
		ADD_METHOD_TO(PlantController::listAdminPlants, "/api/admin/plants", drogon::Get);
		ADD_METHOD_TO(PlantController::createPlant, "/api/admin/plants", drogon::Post);
		ADD_METHOD_TO(PlantController::updatePlant, "/api/admin/plants/{1}", drogon::Patch);
		ADD_METHOD_TO(PlantController::deletePlant, "/api/admin/plants/{1}", drogon::Delete);
	METHOD_LIST_END

	/**
 * Liste publique des plantes.
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 */
	void listPlants(const drogon::HttpRequestPtr& req,
	                std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
 * Details d une plante.
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 * @param plantId Identifiant de la plante
 */
	void getPlant(const drogon::HttpRequestPtr& req,
	              std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	              int plantId);

	/**
 * Liste complete (admin).
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 */
	void listAdminPlants(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
 * Creation d une plante (admin).
 *
 * @param req Requete HTTP avec JSON
 * @param callback Callback de reponse
 */
	void createPlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/**
 * Mise a jour d une plante (admin).
 *
 * @param req Requete HTTP avec JSON
 * @param callback Callback de reponse
 * @param plantId Identifiant de la plante
 */
	void updatePlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int plantId);

	/**
 * Suppression d une plante (admin).
 *
 * @param req Requete HTTP
 * @param callback Callback de reponse
 * @param plantId Identifiant de la plante
 */
	void deletePlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int plantId);
};
