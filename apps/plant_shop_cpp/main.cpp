#include <drogon/drogon.h>
#include "routes.h"
#include <iostream>
#include <fstream>
#include <string>
#include <filesystem>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

using namespace drogon;

/** """ Vérifie si le port d’écoute HTTP est disponible avant le lancement du serveur """ */
bool isPortAvailable(unsigned short port) {
	int sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd < 0) return false;
	sockaddr_in addr{};
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = htonl(INADDR_ANY);
	addr.sin_port = htons(port);
	bool ok = (bind(sockfd, (struct sockaddr*)&addr, sizeof(addr)) == 0);
	close(sockfd);
	return ok;
}
/** """ Vérifie la présence du fichier config.json """ */
void checkConfigFile(const std::string& path = "config.json") {
	if (std::filesystem::exists(path)) {
		std::cout << "✅ Fichier de configuration détecté : "
		          << std::filesystem::absolute(path) << std::endl;
	} else {
		std::cerr << "⚠️  Fichier de configuration introuvable : "
		          << std::filesystem::absolute(path) << std::endl;
	}
}

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

/** """ Extrait les paramètres du DATABASE_URL """ */
void parseDatabaseUrl(const std::string& url, std::string& host, unsigned short& port,
                      std::string& dbname, std::string& user, std::string& pass) {
	if (url.rfind("postgresql://", 0) != 0)
		throw std::runtime_error("DATABASE_URL invalide (préfixe postgresql:// attendu)");

	std::string data = url.substr(13);
	auto atPos = data.find('@');
	if (atPos == std::string::npos) throw std::runtime_error("DATABASE_URL invalide (identifiants manquants)");
	auto creds = data.substr(0, atPos);
	auto rest = data.substr(atPos + 1);

	auto colonCreds = creds.find(':');
	if (colonCreds == std::string::npos) throw std::runtime_error("DATABASE_URL invalide (format user:pass attendu)");
	user = creds.substr(0, colonCreds);
	pass = creds.substr(colonCreds + 1);

	auto slash = rest.find('/');
	if (slash == std::string::npos) throw std::runtime_error("DATABASE_URL invalide (nom de base manquant)");
	dbname = rest.substr(slash + 1);
	auto hostPort = rest.substr(0, slash);

	auto colonHost = hostPort.find(':');
	if (colonHost == std::string::npos) throw std::runtime_error("DATABASE_URL invalide (port manquant)");
	host = hostPort.substr(0, colonHost);
	port = static_cast<unsigned short>(std::stoi(hostPort.substr(colonHost + 1)));
}

/** """ Crée la connexion PostgreSQL à partir du .env """ */
void connectToDatabaseFromEnv() {
	std::string url = db_stringenv_read_secured();
	if (url.empty()) throw std::runtime_error("DATABASE_URL manquant dans .env");

	std::string host, dbname, user, pass;
	unsigned short port = 0;
	parseDatabaseUrl(url, host, port, dbname, user, pass);

	std::ostringstream conn;
	conn << "host=" << host
	     << " port=" << port
	     << " dbname=" << dbname
	     << " user=" << user
	     << " password=" << pass;

	drogon::app().createDbClient(
		"postgresql",
		conn.str(),
		1, "", "", "",
		0, "", "",
		false, "default",
		0.0, false
	);
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
	bool en_test = false;
	try {
		checkConfigFile();
		drogon::app().loadConfigFile("config.json");
		sleep(0.5);

		app().setLogLevel(trantor::Logger::kInfo);
		if (!isPortAvailable(4100)) {
			std::cerr << "❌ Impossible de démarrer : port 4100 déjà utilisé" << std::endl;
			return 1;
		}
		enableCors();

		try {
			connectToDatabaseFromEnv();
			testDbConnection();
		} catch (const std::exception &e) {
			LOG_FATAL << "❌ Échec lors de la connexion PostgreSQL : " << e.what();
			throw;
		}
		registerRoutes();
		LOG_INFO << "✅ Backend Plant Shop (C++) initialisé sur http://localhost:4100";
		if (en_test) {
			drogon::app().setLogPath("");
			drogon::app().setLogLevel(trantor::Logger::kTrace);
		}
		app().run();
		return 0;
	} catch (const std::exception& e) {
		std::cerr << "❌ Erreur critique : " << e.what() << std::endl;
		return 1;
	}
}
