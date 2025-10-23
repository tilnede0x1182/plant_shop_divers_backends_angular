#include <drogon/drogon.h>
#include <json/json.h>
#include <iostream>
#include <stdexcept>
#include <map>
#include <chrono>
#include <iomanip>
#include <sstream>
#include <random>
#include <thread>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <trantor/net/EventLoopThread.h>

/**
 * ======================================================
 * 🧪 Tests End-to-End — C++ clone du test_e2e.rs
 * ======================================================
 */

using namespace drogon;
using namespace std;

// Vérifie si le port 4100 est ouvert en TCP
bool waitForServer(const char* host, unsigned short port, int timeout_ms = 5000) {
	auto start = std::chrono::steady_clock::now();
	while (std::chrono::steady_clock::now() - start < std::chrono::milliseconds(timeout_ms)) {
		int sockfd = socket(AF_INET, SOCK_STREAM, 0);
		if (sockfd < 0) return false;
		sockaddr_in addr{};
		addr.sin_family = AF_INET;
		addr.sin_port = htons(port);
		inet_pton(AF_INET, host, &addr.sin_addr);
		int result = connect(sockfd, (sockaddr*)&addr, sizeof(addr));
		close(sockfd);
		if (result == 0) return true;
		std::this_thread::sleep_for(std::chrono::milliseconds(100));
	}
	return false;
}

// ------------------------------------------------------
// ⚙️ Contexte principal (client HTTP, cookies, base_url)
// ------------------------------------------------------

class TestContext {
private:
    string base_url;
		string api_prefix;
    map<string, string> cookies;
    string timestamp_str;

    trantor::EventLoopThread loopThread;
    HttpClientPtr client;
public:
    	TestContext() : base_url("http://localhost:4100"), api_prefix("/api") {
        // Initialisation du timestamp
        auto now = chrono::system_clock::now();
        auto in_time_t = chrono::system_clock::to_time_t(now);
        stringstream ss;
        ss << put_time(gmtime(&in_time_t), "%Y%m%d%H%M%S");
        timestamp_str = ss.str();

        // Démarre une boucle d’événements dédiée au client HTTP
        loopThread.run();
        client = HttpClient::newHttpClient(base_url, loopThread.getLoop());
    }

    const string& timestamp() const {
        return timestamp_str;
    }

    // --- Gestion des cookies ---
    string cookie(const string& who) {
        if (cookies.count(who)) {
            return cookies[who];
        }
        return "";
    }

		void set_cookie(const string& who, const string& cookie_val) {
			if (cookies.count(who) && !cookies[who].empty())
				cookies[who] += "; " + cookie_val;	// concatène si plusieurs cookies
			else
				cookies[who] = cookie_val;
		}

    // --- Requête générique ---
    Json::Value request(const string& method_str, const string& path, int status, const Json::Value* body, const string& who) {
        auto req = HttpRequest::newHttpRequest();
        req->setPath(api_prefix + path);

        HttpMethod method;
        if (method_str == "GET") method = Get;
        else if (method_str == "POST") method = Post;
        else if (method_str == "PATCH") method = Patch;
        else if (method_str == "DELETE") method = Delete;
        else throw runtime_error("Méthode HTTP non supportée");

        req->setMethod(method);
        // Drogon calcule Content-Length et envoie correctement le corps
        req->setContentTypeCode(drogon::CT_APPLICATION_JSON);

        string user_cookie = cookie(who);
        if (!user_cookie.empty()) {
            req->addHeader("Cookie", user_cookie);
        }

        if (body) {
            Json::StreamWriterBuilder writer;
            writer["indentation"] = "";
            req->setBody(Json::writeString(writer, *body));
        }

        promise<pair<ReqResult, HttpResponsePtr>> prom;
        auto fut = prom.get_future();

        client->sendRequest(req, [&prom](ReqResult resCode, const HttpResponsePtr& res) {
            prom.set_value({resCode, res});
        });

        auto [resCode, res] = fut.get();

        if (resCode != ReqResult::Ok) {
            throw runtime_error("Erreur de connexion: " + path);
        }

        int code = res->statusCode();
				bool isError = code >= 400;

				std::string ct = res->getHeader("content-type");
				bool isJson = (ct.rfind("application/json", 0) == 0) || (ct.find("+json") != std::string::npos);

        // Gestion du cookie de réponse
				for (const auto &kv : res->cookies()) {
					set_cookie(who, kv.first + "=" + kv.second.value());
				}

        // --- Affichage style test_complet.js ---
        cout << (code == status ? "✅" : "❌") << " "
             << left << setw(7) << method_str << path
             << " [" << code << "]" << endl;

        if (code != status) {
            string txt = string(res->getBody());
            throw runtime_error("API " + method_str + " " + path + " -> " + to_string(code) + " (attendu " + to_string(status) + ")\n" + txt);
        }

        Json::Value json_resp;
        if (!isError && !res->getBody().empty()) {
            Json::CharReaderBuilder reader;
            string errs;
            stringstream s(string(res->getBody()));
            if (!Json::parseFromStream(reader, s, &json_resp, &errs)) {
                throw runtime_error("Réponse JSON invalide: " + errs);
            }
        }
        return json_resp;
    }

    // --- Actions Auth ---
    void login(const string& email, const string& password, const string& who) {
        Json::Value creds;
        creds["email"] = email;
        creds["password"] = password;
        request("POST", "/auth/login", 201, &creds, who);
    }

    void registerUser(const string& name, const string& email, const string& password, const string& who) {
        Json::Value user;
        user["name"] = name;
        user["email"] = email;
        user["password"] = password;
        request("POST", "/auth/register", 201, &user, who);
    }

    // --- Assertions ---
    static void assert_eq(const Json::Value& val, const string& key, const Json::Value& expected) {
        if (!val.isMember(key)) {
            throw runtime_error("Objet vide – clé " + key + " recherchée");
        }
        const Json::Value& actual = val[key];
        bool ok = (actual == expected);
        cout << (ok ? "✅" : "❌") << "   ↳ " << key << "=" << actual.toStyledString()
             << " (attendu " << expected.toStyledString() << ")" << endl;
        if (!ok) {
            throw runtime_error("Assertion échouée pour la clé '" + key + "'");
        }
    }

    static void assert_num(const Json::Value& val, const string& key) {
        if (!val.isMember(key) || !val[key].isNumeric()) {
            throw runtime_error("Clé " + key + " n'est pas numérique ou absente");
        }
    }
};

// ------------------------------------------------------
// 🧪 Modules de test
// ------------------------------------------------------

void test_plants(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: PLANTS (admin)" << endl;
    Json::Value plant_data;
    plant_data["name"] = "Test Plant";
    plant_data["price"] = 10;
    plant_data["stock"] = 5;
    auto plant = ctx.request("POST", "/admin/plants", 201, &plant_data, "admin");
    TestContext::assert_num(plant, "id");
    int id = plant["id"].asInt();
    auto get = ctx.request("GET", "/plants/" + to_string(id), 200, nullptr, "admin");
    TestContext::assert_eq(get, "name", plant_data["name"]);
    Json::Value price_update;
    price_update["price"] = 15;
    ctx.request("PATCH", "/admin/plants/" + to_string(id), 200, &price_update, "admin");
    auto check = ctx.request("GET", "/plants/" + to_string(id), 200, nullptr, "admin");
    TestContext::assert_eq(check, "price", 15);
    ctx.request("DELETE", "/admin/plants/" + to_string(id), 200, nullptr, "admin");
}

void test_users(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: USERS (admin)" << endl;
    string email = "utilisateur_test_" + ctx.timestamp() + "@example.com";
    Json::Value user_data;
    user_data["email"] = email;
    user_data["name"] = "Utilisateur de test";
    user_data["password"] = "pass123";
    auto user = ctx.request("POST", "/users", 201, &user_data, "admin");
    int id = user["id"].asInt();
    Json::Value name_update;
    name_update["name"] = "Tester Update";
    ctx.request("PATCH", "/users/" + to_string(id), 200, &name_update, "admin");
    auto get = ctx.request("GET", "/users/" + to_string(id), 200, nullptr, "admin");
    TestContext::assert_eq(get, "name", "Tester Update");
    ctx.request("DELETE", "/users/" + to_string(id), 200, nullptr, "admin");
}

void test_orders(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: ORDERS & ORDER ITEMS" << endl;
    Json::Value plant_data;
    plant_data["name"] = "Plante_de_test_" + ctx.timestamp();
    plant_data["price"] = 10;
    plant_data["stock"] = 5;
    auto plant = ctx.request("POST", "/admin/plants", 201, &plant_data, "admin");
    TestContext::assert_num(plant, "id");
    int pid = plant["id"].asInt();

    Json::Value order_data;
    Json::Value item;
    item["plantId"] = pid;
    item["quantity"] = 2;
    order_data["items"].append(item);
    auto order = ctx.request("POST", "/orders", 201, &order_data, "user");
    TestContext::assert_num(order, "id");
    int oid = order["id"].asInt();

    Json::Value status_update;
    status_update["status"] = "shipped";
    ctx.request("PATCH", "/orders/" + to_string(oid), 200, &status_update, "admin");

    auto list = ctx.request("GET", "/orders", 200, nullptr, "user");
    Json::Value found;
    for (const auto& o : list) {
        if (o["id"].asInt() == oid) {
            found = o;
            break;
        }
    }
    if (found.isNull()) throw runtime_error("Commande absente");

    TestContext::assert_eq(found, "status", "shipped");
    if (!found.isMember("orderItems") || found["orderItems"].empty()) {
        throw runtime_error("Items absents dans la commande");
    }
    TestContext::assert_eq(found["orderItems"][0]["plant"], "name", plant_data["name"]);

    ctx.request("DELETE", "/orders/" + to_string(oid), 200, nullptr, "admin");
    ctx.request("DELETE", "/admin/plants/" + to_string(pid), 200, nullptr, "admin");
}

void test_user_profile(TestContext& ctx, const string& email) {
    cout << "\n📌 TEST MODULE: USER PROFILE (user)" << endl;
    auto users = ctx.request("GET", "/users", 200, nullptr, "admin");
    Json::Value user_obj;
    for (const auto& u : users) {
        if (u["email"].asString() == email) {
            user_obj = u;
            break;
        }
    }
    int uid = user_obj["id"].asInt();
    auto profile = ctx.request("GET", "/users/" + to_string(uid), 200, nullptr, "user");
    TestContext::assert_eq(profile, "id", uid);

    string new_name = "Utilisateur_de_test_" + ctx.timestamp();
    Json::Value name_update;
    name_update["name"] = new_name;
    ctx.request("PATCH", "/users/" + to_string(uid), 200, &name_update, "user");

    auto updated = ctx.request("GET", "/users/" + to_string(uid), 200, nullptr, "user");
    TestContext::assert_eq(updated, "name", new_name);

    Json::Value admin_update;
    admin_update["admin"] = true;
    ctx.request("PATCH", "/users/" + to_string(uid), 200, &admin_update, "user");

    auto check = ctx.request("GET", "/users/" + to_string(uid), 200, nullptr, "admin");
    TestContext::assert_eq(check, "admin", false);
}

void test_auth_roles(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: ROLES" << endl;
    Json::Value bad_plant;
    bad_plant["name"] = "Bad"; bad_plant["price"] = 1; bad_plant["stock"] = 1;
    ctx.request("POST", "/admin/plants", 403, &bad_plant, "user");

    Json::Value good_plant;
    good_plant["name"] = "Good"; good_plant["price"] = 1; good_plant["stock"] = 1;
    auto plant = ctx.request("POST", "/admin/plants", 201, &good_plant, "admin");
    int pid = plant["id"].asInt();
    ctx.request("DELETE", "/admin/plants/" + to_string(pid), 200, nullptr, "admin");

    ctx.request("GET", "/users", 403, nullptr, "user");
}

void test_admin_plants(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: ADMIN PLANTS" << endl;
    auto plantes = ctx.request("GET", "/admin/plants", 200, nullptr, "admin");
    cout << "   ↳ " << plantes.size() << " plantes récupérées" << endl;

    Json::Value plant_data;
    plant_data["name"] = "Plante_admin_" + ctx.timestamp();
    plant_data["price"] = 99;
    plant_data["stock"] = 12;
    auto p = ctx.request("POST", "/admin/plants", 201, &plant_data, "admin");
    int id = p["id"].asInt();

    Json::Value price_update;
    price_update["price"] = 123;
    ctx.request("PATCH", "/admin/plants/" + to_string(id), 200, &price_update, "admin");
    ctx.request("DELETE", "/admin/plants/" + to_string(id), 200, nullptr, "admin");
}

void test_admin_users(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: ADMIN USERS" << endl;
    string email = "admin_temp_" + ctx.timestamp() + "@example.com";
    string name = "Admin Temporaire " + ctx.timestamp();

    Json::Value temp_admin_data;
    temp_admin_data["email"] = email;
    temp_admin_data["name"] = name;
    temp_admin_data["password"] = "password";
    temp_admin_data["admin"] = true;
    auto temp = ctx.request("POST", "/users", 201, &temp_admin_data, "admin");
    int id = temp["id"].asInt();

    auto list = ctx.request("GET", "/admin/users", 200, nullptr, "admin");
    Json::Value cible;
    for(const auto& u : list) {
        if(u["email"].asString() == email) {
            cible = u;
            break;
        }
    }
    if (cible.isNull()) throw runtime_error("L'admin temporaire n'a pas été trouvé dans la liste !");
    TestContext::assert_eq(cible, "name", name);

    string nouveau_nom = "Admin_temp_modifié_" + ctx.timestamp();
    Json::Value name_update;
    name_update["name"] = nouveau_nom;
    ctx.request("PATCH", "/users/" + to_string(id), 200, &name_update, "admin");

    auto user_get = ctx.request("GET", "/users/" + to_string(id), 200, nullptr, "admin");
    TestContext::assert_eq(user_get, "name", nouveau_nom);

    ctx.request("DELETE", "/users/" + to_string(id), 200, nullptr, "admin");
}

void test_auth_me(TestContext& ctx) {
    cout << "\n📌 TEST MODULE: AUTH /me" << endl;
    auto me = ctx.request("GET", "/auth/me", 200, nullptr, "user");
    string mail = me["email"].asString();
    string nom = me["name"].asString();
    TestContext::assert_eq(me, "email", mail);
    TestContext::assert_eq(me, "name", nom);
    cout << "   ↳ Utilisateur connecté: " << mail << " (" << nom << ")" << endl;
}


// ------------------------------------------------------
// 🚀 Exécution principale
// ------------------------------------------------------
int main() {
    try {
				// Attendre que le serveur soit prêt avant les requêtes
				if (!waitForServer("127.0.0.1", 4100, 8000)) {
					std::cerr << "❌ Serveur http://localhost:4100 injoignable après 8s" << std::endl;
					return 2;
				}
        TestContext ctx;
        const string admin_email = "admin1@planteshop.com";
        const string admin_password = "password";

        // Génération d'un tag aléatoire comme en Rust
        random_device rd;
        mt19937 gen(rd());
        uniform_int_distribution<> distrib(0, 35);
        string random_tag;
        for(int i=0; i<4; ++i) {
            int val = distrib(gen);
            if (val < 26) random_tag += (char)('a' + val);
            else random_tag += (char)('0' + (val - 26));
        }

        string user_email = "utilisateur_de_test_" + ctx.timestamp() + "_" + random_tag + "@example.com";
        string user_password = "pass123";

        cout << "🧪 Démarrage des tests: http://localhost:4100/api\n" << endl;

        ctx.login(admin_email, admin_password, "admin");
        ctx.registerUser("User", user_email, user_password, "user");
        ctx.login(user_email, user_password, "user");

        test_plants(ctx);
        test_users(ctx);
        test_orders(ctx);
        test_user_profile(ctx, user_email);
        test_auth_roles(ctx);
        test_admin_plants(ctx);
        test_admin_users(ctx);
        test_auth_me(ctx);

        cout << "\n🎉 Tous les tests ont réussi!" << endl;
        return 0;
    } catch (const exception& e) {
        cerr << "\n❌ Tests interrompus: " << e.what() << endl;
        return 1;
    }
}
