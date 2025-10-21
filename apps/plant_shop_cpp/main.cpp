#include <drogon/drogon.h>
#include "routes.h"
#include <iostream>
#include <fstream>
#include <string>

/** """ Lit .env et retourne la valeur de DATABASE_URL """ */
std::string db_stringenv_read() {
	std::ifstream envFile(".env");
	if (!envFile.is_open()) return "";
	std::string line;
	while (std::getline(envFile, line)) {
		if (line.rfind("DATABASE_URL=", 0) == 0)
			return line.substr(13);
	}
	return "";
}

/** """ Lecture sécurisée du .env """ */
std::string db_stringenv_read_secured() {
	try {
		std::string dbUrl = db_stringenv_read();
		if (dbUrl.empty()) {
			std::cerr << "❌ Échec : DATABASE_URL introuvable dans .env" << std::endl;
			return "";
		}
		std::cout << "✅ Lecture réussie : " << dbUrl << std::endl;
		return dbUrl;
	} catch (const std::exception& e) {
		std::cerr << "❌ Erreur inattendue : " << e.what() << std::endl;
		return "";
	}
}

/** """ Active CORS pour Angular """ */
void enableCors() {
	using namespace drogon;
	app().enableSession();
	app().registerPostHandlingAdvice([](const HttpRequestPtr&, const HttpResponsePtr& resp) {
		resp->addHeader("Access-Control-Allow-Origin", "*");
		resp->addHeader("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
		resp->addHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
	});
}

/** """ Crée le client PostgreSQL """ */
void createDbClient(const std::string& dbUrl) {
	using namespace drogon;
	app().createDbClient(
		"postgresql",  // type de base
		dbUrl,         // URL complète (provenant du .env)
		1,             // nombre de connexions (client global unique)
		"", "", "",    // user, password, dbname
		0, "", "",     // port, charset, filename
		false, "",     // isFast, appName
		0.0, false     // timeout, autoReconnect
	);
	LOG_INFO << "✅ Client PostgreSQL (global) initialisé : " << dbUrl;
}

/** """ Teste la connexion PostgreSQL """ */
void testDbConnection() {
	using namespace std::chrono_literals;
	try {
		std::shared_ptr<drogon::orm::DbClient> db = nullptr;

		for (int i = 0; i < 5; ++i) {
			db = drogon::app().getDbClient();
			if (db) break;
			std::this_thread::sleep_for(std::chrono::milliseconds(100));
		}
		if (!db) {
			LOG_ERROR << "⚠️ Client DB non disponible avant le démarrage du serveur. "
								<< "Connexion testée plus tard dans app().run().";
			return; // on quitte sans lever d’exception ici
		}

		// Validation du client uniquement si accessible
		try {
			db->execSqlAsyncFuture("SELECT 1").get();
			LOG_INFO << "✅ Connexion PostgreSQL testée avec succès (SELECT 1)";
		} catch (const std::exception &ex) {
			LOG_WARN << "⚠️ Connexion PostgreSQL non encore prête : " << ex.what();
		}
	} catch (const std::exception& e) {
		LOG_FATAL << "❌ Test de connexion PostgreSQL échoué : " << e.what();
		throw;
	}
}

/** """ Point d’entrée principal du backend Plant Shop (C++) """ */
int main() {
	try {
		using namespace drogon;
		app().setLogLevel(trantor::Logger::kInfo);
		app().addListener("0.0.0.0", 4100);
		enableCors();

		std::string dbUrl = db_stringenv_read_secured();
		if (dbUrl.empty()) return 1;
		LOG_INFO << "🔹 DATABASE_URL chargé depuis le fichier .env";

		try {
			createDbClient(dbUrl);
			try {
				testDbConnection();
			} catch (const std::exception& e) {
				LOG_FATAL << "❌ Test de connexion PostgreSQL échoué : " << e.what();
				throw;
			}
		} catch (const std::exception &e) {
			LOG_FATAL << "❌ Impossible de créer le client DB : " << e.what();
			throw;
		}

		registerRoutes();
		LOG_INFO << "✅ Backend Plant Shop (C++) initialisé sur http://localhost:4100";
		app().run();
		return 0;
	} catch (const std::exception& e) {
		std::cerr << "❌ Erreur critique : " << e.what() << std::endl;
		return 1;
	}
}


// #include <drogon/drogon.h>
// #include "routes.h"

// /**
//  * """ Point d’entrée principal du backend Plant Shop (C++)
//  *  - Initialise la configuration serveur Drogon
//  *  - Enregistre les routes depuis routes.h
//  *  - Connecte la base PostgreSQL (url via .env ou variable)
//  *  - Active CORS pour le frontend Angular
//  *  - Démarre le serveur HTTP sur le port 4100
//  * """
//  */
// int main() {
//         try {
//                 // ───────────────────────────────
//                 //   Configuration générale
//                 // ───────────────────────────────
//                 using namespace drogon;
//                 app().setLogLevel(trantor::Logger::kInfo);

//                 // Port et adresse
//                 app().addListener("0.0.0.0", 4100);

//                 // CORS activé (Angular en local)
//                 app().enableSession();
//                 app().registerPostHandlingAdvice([](const HttpRequestPtr&, const HttpResponsePtr& resp) {
//                         resp->addHeader("Access-Control-Allow-Origin", "*");
//                         resp->addHeader("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
//                         resp->addHeader("Access-Control-Allow-Headers", "Content-Type,Authorization");
//                 });

//                 // ───────────────────────────────
//                 //   Connexion base PostgreSQL
//                 // ───────────────────────────────
//                 // Exemple : postgres://user:pass@localhost:5432/plant_shop
//                 std::string dbUrl = std::getenv("DATABASE_URL") ?
//                         std::getenv("DATABASE_URL") :
//                         "postgresql://postgres:postgres@localhost:5432/plant_shop";

//                 // Correction : signature complète pour compatibilité avec Drogon >= 1.9
// 								app().createDbClient(
// 												"postgresql",  // type de base
// 												dbUrl,         // URL complète
// 												1,             // nombre de connexions
// 												"", "", "",    // user, password, dbname
// 												0, "", "",     // port, charset, filename
// 												false, "",     // isFast, appName
// 												0.0, false     // timeout, autoReconnect
// 								);

// 								// Attente de disponibilité du client global (max 5 s)
// 								std::shared_ptr<drogon::orm::DbClient> db = nullptr;
// 								for (int i = 0; i < 50; ++i) {
// 												db = app().getDbClient();
// 												if (db) break;
// 												std::this_thread::sleep_for(std::chrono::milliseconds(100));
// 								}
// 								if (!db)
// 												throw std::runtime_error("Client DB global non accessible après délai");
// 								db->execSqlAsyncFuture("SELECT 1").get();
// 								LOG_INFO << "✅ Connexion PostgreSQL testée avec succès (SELECT 1)";

//                 // ───────────────────────────────
//                 //   Enregistrement des routes
//                 // ───────────────────────────────
//                 registerRoutes();

//                 LOG_INFO << "✅ Backend Plant Shop (C++) initialisé sur http://localhost:4100";

//                 // ───────────────────────────────
//                 //   Lancement du serveur
//                 // ───────────────────────────────
//                 app().run();

//                 return 0;
//         } catch (const std::exception& e) {
//                 std::cerr << "❌ Erreur critique : " << e.what() << std::endl;
//                 return 1;
//         }
// }
