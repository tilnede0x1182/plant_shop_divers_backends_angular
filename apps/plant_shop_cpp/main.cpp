#include <drogon/drogon.h>
#include "routes.h"

/**
 * """ Point d’entrée principal du backend Plant Shop (C++)
 *  - Initialise la configuration serveur Drogon
 *  - Enregistre les routes depuis routes.h
 *  - Connecte la base PostgreSQL (url via .env ou variable)
 *  - Active CORS pour le frontend Angular
 *  - Démarre le serveur HTTP sur le port 4100
 * """
 */
int main() {
	try {
		// ───────────────────────────────
		//   Configuration générale
		// ───────────────────────────────
		using namespace drogon;
		app().setLogLevel(trantor::Logger::kInfo);

		// Port et adresse
		app().addListener("0.0.0.0", 4100);

		// CORS activé (Angular en local)
		app().enableSession();
		app().registerPostHandlingAdvice([](const HttpRequestPtr&, const HttpResponsePtr& resp) {
			resp->addHeader("Access-Control-Allow-Origin", "*");
			resp->addHeader("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
			resp->addHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
		});

		// ───────────────────────────────
		//   Connexion base PostgreSQL
		// ───────────────────────────────
		// Exemple : postgres://user:pass@localhost:5432/plant_shop
		std::string dbUrl = std::getenv("DATABASE_URL") ?
			std::getenv("DATABASE_URL") :
			"postgresql://postgres:postgres@localhost:5432/plant_shop";

		// Correction : signature complète pour compatibilité avec Drogon >= 1.9
		app().createDbClient(
			"postgresql",  // type de base
			dbUrl,         // URL complète
			1,             // nombre de connexions
			"", "", "",    // user, password, dbname
			0, "", "",     // port, charset, filename
			false, "",     // isFast, appName
			0.0, false     // timeout, autoReconnect
		);

		// ───────────────────────────────
		//   Enregistrement des routes
		// ───────────────────────────────
		registerRoutes();

		LOG_INFO << "✅ Backend Plant Shop (C++) initialisé sur http://localhost:4100";

		// ───────────────────────────────
		//   Lancement du serveur
		// ───────────────────────────────
		app().run();

		return 0;
	} catch (const std::exception& e) {
		std::cerr << "❌ Erreur critique : " << e.what() << std::endl;
		return 1;
	}
}
