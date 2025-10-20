#include <drogon/drogon.h>
#include <drogon/orm/Mapper.h>
#include <json/json.h>
#include <fstream>
#include <random>
#include <chrono>
#include <iomanip>
#include <sstream>
using namespace drogon;
using namespace drogon::orm;
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

/** """ Hash mot de passe (stub). Remplacez par bcrypt(cost=10) si nécessaire.
	@plain mot de passe clair """ */
static string hashPassword(const string& plain) {
	// TODO: implémenter bcrypt (coût 10) si votre backend le requiert.
	// La seed.ts utilise bcrypt.hash(password, 10). :contentReference[oaicite:1]{index=1}
	return plain;
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
		"INSERT INTO users (email, name, password, admin) VALUES ($1,$2,$3,$4)",
		outEmail,
		"Admin " + std::to_string(index + 1),
		hashPassword(outPwd),
		true
	);
}

/** """ Crée NB_ADMINS admins, retourne (email, password) """ */
static vector<std::pair<string,string>> createAdmins(DbClientPtr db) {
	vector<std::pair<string,string>> creds;
	for (int i=0;i<NB_ADMINS;i++) {
		string email, pwd;
		addAdmin(db, i, email, pwd);
		creds.emplace_back(email, pwd);
	}
	return creds;
}

/** """ Ajoute un user aléatoire (faker.email + name + password)
	@db client
	@return (email,password) """ */
static std::pair<string,string> addUser(DbClientPtr db) {
	// email simple (lowercase) et mot de passe long (12)
	const string email = "user_" + std::to_string(rndInt(100000,999999)) + "@example.com";
	const string pwd = "pw" + std::to_string(rndInt(100000000, 999999999));
	const string name = "User " + std::to_string(rndInt(1000,9999));
	db->execSqlSync(
		"INSERT INTO users (email, name, password, admin) VALUES ($1,$2,$3,$4)",
		email, name, hashPassword(pwd), false
	);
	return {email, pwd};
}

/** """ Crée NB_USERS utilisateurs et renvoie (email,password) """ */
static vector<std::pair<string,string>> createUsers(DbClientPtr db) {
	vector<std::pair<string,string>> creds;
	for (int i=0;i<NB_USERS;i++) {
		creds.emplace_back(addUser(db));
	}
	return creds;
}

/** """ Ajoute une plante (nom, prix 5..50, description, stock 5..30)
	@db client
	@name nom
	@return id créé """ */
static int addPlant(DbClientPtr db, const string& name) {
	int price = rndInt(5,50);
	int stock = rndInt(5,30);
	auto result = db->execSqlSync(
		"INSERT INTO plants (name, price, description, stock) VALUES ($1,$2,$3,$4) RETURNING id",
		name, price, loremSentence(), stock
	);
	return result->get(0)["id"].as<int>();
}

/** """ Crée NB_PLANTS plantes en recyclant PLANT_NAMES (suffixe “X” si > base)
	@db client
	@return liste {id,name,price,stock} minimaliste pour la suite """ */
struct PlantRow { int id; string name; int price; int stock; };
static vector<PlantRow> createPlants(DbClientPtr db) {
	const int max = (int)(sizeof(PLANT_NAMES)/sizeof(PLANT_NAMES[0]));
	vector<PlantRow> out;
	out.reserve(NB_PLANTS);
	for (int i=0;i<NB_PLANTS;i++) {
		string base = PLANT_NAMES[i % max];
		string name = (NB_PLANTS > max) ? (base + string(" ") + std::to_string((i / max) + 1)) : base;
		int id = addPlant(db, name);
		// relire price/stock pour suivre la décrémentation ultérieure
		auto row = db->execSqlSync("SELECT price,stock FROM plants WHERE id=$1", id);
		out.push_back({id,name,row->get(0)["price"].as<int>(), row->get(0)["stock"].as<int>()});
	}
	return out;
}

/** """ Ajoute un item à une commande: choisit une plante, borne qty 1..5 <= stock
	@db client
	@orderId id commande
	@plants ref modifiable (stock décrémenté en mémoire)
	@return total ajouté """ */
static int addItem(DbClientPtr db, int orderId, vector<PlantRow>& plants) {
	if (plants.empty()) return 0;
	const int idx = rndInt(0, (int)plants.size()-1);
	auto& p = plants[idx];
	if (p.stock <= 0) return 0;
	int qty = std::min(rndInt(1,5), p.stock);
	if (qty <= 0) return 0;

	db->execSqlSync("INSERT INTO order_items (order_id, plant_id, quantity) VALUES ($1,$2,$3)", orderId, p.id, qty);
	db->execSqlSync("UPDATE plants SET stock = stock - $1 WHERE id = $2", qty, p.id);
	p.stock -= qty;
	return p.price * qty;
}

/** """ Crée une commande pour un user avec 2 items, met à jour total
	@db client
	@userId id user
	@plants plantes (stock décrémenté)
	@status aléatoire parmi {confirmed,pending,shipped,delivered} """ */
static void createOrderForUser(DbClientPtr db, int userId, vector<PlantRow>& plants) {
	static const char* statuses[] = {"confirmed","pending","shipped","delivered"};
	const string st = statuses[rndInt(0,3)];
	auto orderRow = db->execSqlSync(
		"INSERT INTO orders (user_id, total_price, status) VALUES ($1,$2,$3) RETURNING id",
		userId, 0, st
	);
	int orderId = orderRow->get(0)["id"].as<int>();

	int total = 0;
	for (int k=0;k<2;k++) total += addItem(db, orderId, plants);
	db->execSqlSync("UPDATE orders SET total_price=$1 WHERE id=$2", total, orderId);
}

/** """ Crée de 0..MAX_ORDERS_PER_USER commandes pour chaque user
	@db client
	@plants plantes """ */
static void createOrders(DbClientPtr db, vector<PlantRow> plants) {
	auto users = db->execSqlSync("SELECT id FROM users");
	for (size_t i=0;i<users->rows();i++) {
		int userId = users->get(i)["id"].as<int>();
		int n = rndInt(0, MAX_ORDERS_PER_USER);
		for (int k=0;k<n;k++) createOrderForUser(db, userId, plants);
	}
}

/** """ Écrit users.txt (admins puis utilisateurs)
	@admins paires email/password
	@users paires email/password """ */
static void writeUsersFile(const vector<std::pair<string,string>>& admins,
                           const vector<std::pair<string,string>>& users) {
	std::ofstream f("users.txt", std::ios::out | std::ios::trunc);
	if (!f) return;
	f << "Administrateurs :\n\n";
	for (auto& a : admins) f << a.first << ' ' << a.second << "\n";
	f << "\nUtilisateurs :\n\n";
	for (auto& u : users) f << u.first << ' ' << u.second << "\n";
}

/** """ run(): reset → admins → users → plants → users.txt → orders (fidèle seed.ts)
	@db client Drogon """ */
static void run(DbClientPtr db) {
	reset(db);
	auto admins = createAdmins(db);
	auto users  = createUsers(db);
	auto plants = createPlants(db);
	writeUsersFile(admins, users);
	createOrders(db, plants);
}

/** """ main(): utilise la config DB de Drogon (config.json / env), exécute la seed """ */
int main() {
	try {
		// Si vous avez un config.json, vous pouvez charger: app().loadConfigFile("config.json");
		auto db = app().getDbClient();
		if (!db) {
			// Fallback: créer un client à la volée via env DATABASE_URL si nécessaire
			const char* url = std::getenv("DATABASE_URL");
			if (url && *url) {
				auto client = drogon::orm::DbClient::newClient(url, 1);
				run(client);
			} else {
				throw std::runtime_error("DATABASE_URL manquant et aucun DbClient configuré");
			}
		} else {
			run(db);
		}
		std::cout << "✅ Seed terminée. Données créées & users.txt généré.\n";
		return 0;
	} catch (const std::exception& e) {
		std::cerr << "❌ Seed échouée: " << e.what() << "\n";
		return 1;
	}
}
