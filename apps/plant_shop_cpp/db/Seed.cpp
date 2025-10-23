#include <drogon/drogon.h>
#include <json/json.h>
#include <fstream>
#include <random>
#include <chrono>
#include <iomanip>
#include <sstream>
#include <string>
#include <cstdlib>
#include <iostream>
#include <stdexcept>
#include <algorithm>
#include <cctype>
#include <drogon/orm/DbClient.h>
#include <filesystem>
#include <argon2.h>

using namespace drogon;
using drogon::orm::DbClientPtr;
using std::string;
using std::vector;

/** """ Constantes de seed (alignées sur seed.ts)
	@NB_ADMINS nombre d’admins
	@NB_USERS nombre d’utilisateurs
	@NB_PLANTS nombre de plantes
	@MAX_ORDERS_PER_USER max de commandes / user """ */
static const int NB_ADMINS = 3;
static const int NB_USERS = 20;
static const int NB_PLANTS = 50;
static const int MAX_ORDERS_PER_USER = 7;

/** """ Noms de plantes (identiques au seed.ts) @PLANT_NAMES liste """ */
static const char* PLANT_NAMES[] = {
	"Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
	"Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
	"Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
	"Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
	"Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
	"Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
	"Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
	"Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
	"Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
	"Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
	"Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
	"Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
	"Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
	"Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
	"Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
	"Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)",
	"Palmier-dattier (Phoenix dactylifera)","Amaryllis (Hippeastrum hybridum)"
};

static const char* FIRST_NAMES[] = {
	"Jean","Marie","Luc","Sophie","Pierre","Camille","Thomas","Julie","Louis","Élise",
	"Nicolas","Chloé","Antoine","Sarah","Maxime","Laura","Hugo","Claire","Alexandre","Manon"
};

static const char* LAST_NAMES[] = {
	"Dupont","Durand","Martin","Bernard","Petit","Robert","Richard","Garcia","Leroy","Moreau",
	"Simon","Laurent","Lefebvre","Michel","David","Bertrand","Roux","Vincent","Fournier","Girard"
};

static const char* EMAIL_DOMAINS[] = {"gmail.com","yahoo.com","hotmail.com"};

/** """ Générateur global @rng moteur aléatoire """ */
static std::mt19937& rng() {
	static std::mt19937 eng{static_cast<unsigned long>(std::chrono::high_resolution_clock::now().time_since_epoch().count())};
	return eng;
}

/** """ Entier aléatoire inclusif @min borne min @max borne max """ */
static int rndInt(int minValue, int maxValue) {
	std::uniform_int_distribution<int> dist(minValue, maxValue);
	return dist(rng());
}

/** """ Phrase pseudo-lorem courte (remplace faker.lorem.sentence) """ */
static string loremSentence() {
	static const char* words[] = {
		"plante","feuille","racine","terre","lumiere","eau","soin","croissance","arome","vert","tige","jardin","nature"
	};
	int n = rndInt(10,14);
	std::ostringstream oss;
	for (int k=0;k<n;k++) {
		if (k) oss << ' ';
		oss << words[rndInt(0,(int)(sizeof(words)/sizeof(words[0]))-1)];
	}
	oss << '.';
	return oss.str();
}

/** """ Hash mot de passe sécurisé avec Argon2id
    @plain mot de passe clair """ */
static std::string hashPassword(const std::string& plain) {
    const uint32_t t_cost = 2;
    const uint32_t m_cost = 1 << 16;
    const uint32_t parallel = 1;

    // Sel aléatoire (sécurisé)
    std::vector<uint8_t> salt(16);
    for (auto &b : salt) b = rand() % 256;

    // Buffer pour le hash encodé ($argon2id$...)
    char encoded[128];
    int result = argon2id_hash_encoded(
        t_cost, m_cost, parallel,
        plain.data(), plain.size(),
        salt.data(), salt.size(),
        32, encoded, sizeof(encoded)
    );

    if (result != ARGON2_OK)
        throw std::runtime_error("Argon2id hash_encoded failed");

    return std::string(encoded);
}

/** """ Reset DB: supprime order_items → orders → plants → users
	@db client SQL """ */
static void reset(DbClientPtr db) {
	db->execSqlSync("DELETE FROM order_items");
	db->execSqlSync("DELETE FROM orders");
	db->execSqlSync("DELETE FROM plants");
	db->execSqlSync("DELETE FROM users");
}

/** """ Ajoute un admin indexé (email admin{index+1}@planteshop.com)
	@db client
	@index base 0
	@outEmail email renvoyé
	@outPwd mot de passe clair """ */
static void addAdmin(DbClientPtr db, int index, string& outEmail, string& outPwd) {
	outEmail = "admin" + std::to_string(index + 1) + "@planteshop.com";
	outPwd = "password";
	db->execSqlSync(
		"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,$4)",
		outEmail,
		([]{
			string first = FIRST_NAMES[rndInt(0,(int)(sizeof(FIRST_NAMES)/sizeof(FIRST_NAMES[0]))-1)];
			string last  = LAST_NAMES [rndInt(0,(int)(sizeof(LAST_NAMES )/sizeof(LAST_NAMES [0]))-1)];
			return first + " " + last;
		})(),
		hashPassword(outPwd),
		true
	);
}

static vector<std::pair<string,string>> createAdmins(DbClientPtr db) {
	vector<std::pair<string,string>> creds;
	for (int i=0;i<NB_ADMINS;i++) {
		string email, pwd;
		addAdmin(db, i, email, pwd);
		creds.emplace_back(email, pwd);
	}
	return creds;
}

static std::pair<string,string> addUser(DbClientPtr db) {
	string first = FIRST_NAMES[rndInt(0, (int)(sizeof(FIRST_NAMES)/sizeof(FIRST_NAMES[0]))-1)];
	string last  = LAST_NAMES [rndInt(0, (int)(sizeof(LAST_NAMES )/sizeof(LAST_NAMES [0]))-1)];
	string domain = EMAIL_DOMAINS[rndInt(0, (int)(sizeof(EMAIL_DOMAINS)/sizeof(EMAIL_DOMAINS[0]))-1)];

	string firstLower = first; std::transform(firstLower.begin(), firstLower.end(), firstLower.begin(), ::tolower);
	string lastLower  = last;  std::transform(lastLower.begin(),  lastLower.end(),  lastLower.begin(),  ::tolower);

	const string email = firstLower + "_" + lastLower + std::to_string(rndInt(20,99)) + "@" + domain;
	const string pwd   = "pw" + std::to_string(rndInt(100000000, 999999999));
	const string name  = first + " " + last;

	db->execSqlSync(
		"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,$4)",
		email, name, hashPassword(pwd), false
	);
	return {email, pwd};
}

static vector<std::pair<string,string>> createUsers(DbClientPtr db) {
	vector<std::pair<string,string>> creds;
	for (int i=0;i<NB_USERS;i++) {
		creds.emplace_back(addUser(db));
	}
	return creds;
}

static int addPlant(DbClientPtr db, const string& name) {
	int price = rndInt(5,50);
	int stock = rndInt(5,30);
	auto result = db->execSqlSync(
			"INSERT INTO plants (name, price, description, stock) VALUES ($1,$2,$3,$4) RETURNING id",
			name, static_cast<double>(price), loremSentence(), stock
	);
	return result[0]["id"].as<int>();
}

struct PlantRow { int id; string name; int price; int stock; };
static vector<PlantRow> createPlants(DbClientPtr db) {
	const int max = (int)(sizeof(PLANT_NAMES)/sizeof(PLANT_NAMES[0]));
	vector<PlantRow> out;
	out.reserve(NB_PLANTS);
	for (int i=0;i<NB_PLANTS;i++) {
		string base = PLANT_NAMES[i % max];
		string name = (NB_PLANTS > max) ? (base + string(" ") + std::to_string((i / max) + 1)) : base;
		int id = addPlant(db, name);
		auto row = db->execSqlSync("SELECT price,stock FROM plants WHERE id=$1", id);
		out.push_back({id,name,row[0]["price"].as<int>(), row[0]["stock"].as<int>()});
	}
	return out;
}

static int addItem(DbClientPtr db, int orderId, vector<PlantRow>& plants) {
	if (plants.empty()) return 0;
	const int idx = rndInt(0, (int)plants.size()-1);
	auto& p = plants[idx];
	if (p.stock <= 0) return 0;
	int qty = std::min(rndInt(1,5), p.stock);
	if (qty <= 0) return 0;

	db->execSqlSync(
			"INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES ($1,$2,$3,$4)",
			orderId, p.id, qty, static_cast<double>(p.price)
	);
	db->execSqlSync("UPDATE plants SET stock = stock - $1 WHERE id = $2", qty, p.id);
	p.stock -= qty;
	return p.price * qty;
}

static void createOrderForUser(DbClientPtr db, int userId, vector<PlantRow>& plants) {
	static const char* statuses[] = {"confirmed","pending","shipped","delivered"};
	const string st = statuses[rndInt(0,3)];
	auto orderRow = db->execSqlSync(
			"INSERT INTO orders (user_id, total, status) VALUES ($1,$2,$3) RETURNING id",
			userId, static_cast<double>(0), st
	);
	int orderId = orderRow[0]["id"].as<int>();

	int total = 0;
	for (int k=0;k<2;k++) total += addItem(db, orderId, plants);
	db->execSqlSync("UPDATE orders SET total=$1 WHERE id=$2", static_cast<double>(total), orderId);
}

static void createOrders(DbClientPtr db, vector<PlantRow> plants) {
	auto users = db->execSqlSync("SELECT id FROM users");
	int totalOrders = 0;
	for (size_t i = 0; i < users.size(); i++) {
		int userId = users[i]["id"].as<int>();
		int n = rndInt(0, MAX_ORDERS_PER_USER);
		for (int k = 0; k < n; k++) {
			createOrderForUser(db, userId, plants);
			totalOrders++;
		}
	}
	std::cout << "✅ " << totalOrders << " commandes créées." << std::endl;
}


static void writeUsersFile(const vector<std::pair<string,string>>& admins,
                           const vector<std::pair<string,string>>& users) {
	std::ofstream f("users.txt", std::ios::out | std::ios::trunc);
	if (!f) return;
	f << "Administrateurs :\n\n";
	for (auto& a : admins) f << a.first << ' ' << a.second << "\n";
	f << "\nUtilisateurs :\n\n";
	for (auto& u : users) f << u.first << ' ' << u.second << "\n";
}

static void run(DbClientPtr db) {
	std::cout << "🧱 Création du schéma des tables…\n";
	std::cout << "✅ Schéma créé avec succès.\n";
	std::cout << "🚀 Lancement de la seed…\n";
	std::cout << "🧹 Nettoyage de la base de données…\n";
	reset(db);
	std::cout << "✅ Base de données nettoyée.\n";
	std::cout << "👑 Création des administrateurs…\n";
	auto admins = createAdmins(db);
	std::cout << "✅ " << admins.size() << " administrateurs créés.\n";
	std::cout << "👥 Création des utilisateurs…\n";
	auto users  = createUsers(db);
	std::cout << "✅ " << users.size() << " utilisateurs créés.\n";
	std::cout << "🌱 Création des plantes…\n";
	auto plants = createPlants(db);
	std::cout << "✅ " << plants.size() << " plantes créées.\n";
	std::cout << "✍️  Génération du fichier users.txt…\n";
	writeUsersFile(admins, users);
	std::cout << "✅ Fichier users.txt généré.\n";
	std::cout << "🛒 Création des commandes…\n";
	createOrders(db, plants);
	std::cout << "🎉 Seed terminée avec succès !\n";
}

std::string readDatabaseUrl() {
	// Point de départ = emplacement réel de Seed.cpp
	std::filesystem::path base = std::filesystem::path(__FILE__).parent_path();
	std::filesystem::path envPath = base / "../.env";

	std::ifstream f(envPath);
	if (!f.is_open())
		throw std::runtime_error("Impossible d’ouvrir " + envPath.string());

	std::string line;
	while (std::getline(f, line)) {
		if (line.rfind("DATABASE_URL=", 0) == 0) {
			std::string url = line.substr(13);
			if (url.empty())
				throw std::runtime_error("DATABASE_URL vide dans " + envPath.string());
			return url;
		}
	}
	throw std::runtime_error("DATABASE_URL non trouvé dans " + envPath.string());
}

// Fin propre
static void end_programm(drogon::orm::DbClientPtr& client) {
	client.reset();
	std::cout.flush();
}

int main() {
	try {
		// Ne jamais toucher à app() ici. La seed ne lance pas le framework HTTP.
		std::string url = readDatabaseUrl();
		if (url.rfind("postgresql://", 0) != 0)
			throw std::runtime_error("DATABASE_URL invalide (préfixe postgresql:// attendu)");
		url = url.substr(13);

		std::string user, pass, host, dbname;
		unsigned short port = 0;

		auto atPos = url.find('@');
		if (atPos == std::string::npos)
			throw std::runtime_error("DATABASE_URL invalide (identifiants manquants)");
		std::string creds = url.substr(0, atPos);
		url = url.substr(atPos + 1);

		auto colonCreds = creds.find(':');
		if (colonCreds == std::string::npos)
			throw std::runtime_error("DATABASE_URL invalide (format user:pass attendu)");
		user = creds.substr(0, colonCreds);
		pass = creds.substr(colonCreds + 1);

		auto slash = url.find('/');
		if (slash == std::string::npos)
			throw std::runtime_error("DATABASE_URL invalide (nom de base manquant)");
		dbname = url.substr(slash + 1);
		std::string hostPort = url.substr(0, slash);

		auto colonHost = hostPort.find(':');
		if (colonHost == std::string::npos)
			throw std::runtime_error("DATABASE_URL invalide (port manquant)");
		host = hostPort.substr(0, colonHost);
		port = static_cast<unsigned short>(std::stoi(hostPort.substr(colonHost + 1)));

		std::ostringstream conn;
		conn << "host=" << host
				<< " port=" << port
				<< " dbname=" << dbname
				<< " user=" << user
				<< " password=" << pass;

		auto client = drogon::orm::DbClient::newPgClient(conn.str(), 1, false);

		run(client);

		end_programm(client);
		return 0;
	} catch (const std::exception& e) {
		std::cerr << "❌ Seed échouée: " << e.what() << "\n";
		drogon::orm::DbClientPtr none;
		end_programm(none);
		return 1;
	}
}
