#pragma once
#include <drogon/drogon.h>
#include <json/json.h>
#include "../models/Plant.h"

/**
 * """ Contrôleur des plantes
 *  Routes gérées :
 *   - GET  /api/plants
 *   - GET  /api/plants/{id}
 *   - GET  /api/admin/plants
 *   - POST /api/admin/plants
 *   - PATCH /api/admin/plants/{id}
 *   - DELETE /api/admin/plants/{id}
 * """
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

	/** """ Liste publique des plantes """ */
	void listPlants(const drogon::HttpRequestPtr& req,
	                std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Détails d'une plante """ */
	void getPlant(const drogon::HttpRequestPtr& req,
	              std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	              int plantId);

	/** """ Liste complète (admin) """ */
	void listAdminPlants(const drogon::HttpRequestPtr& req,
	                     std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Création d'une plante (admin) """ */
	void createPlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback);

	/** """ Mise à jour d'une plante (admin) """ */
	void updatePlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int plantId);

	/** """ Suppression d'une plante (admin) """ */
	void deletePlant(const drogon::HttpRequestPtr& req,
	                 std::function<void(const drogon::HttpResponsePtr&)>&& callback,
	                 int plantId);
};
