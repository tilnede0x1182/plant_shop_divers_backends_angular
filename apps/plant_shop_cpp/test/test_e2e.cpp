#include <drogon/drogon.h>
#include <json/json.h>
#include <iostream>
#include <stdexcept>
#include <map>
#include <chrono>
#include <regex>
#include <sstream>
#include <trantor/net/EventLoopThread.h>

/**
 * Test End-to-End complet (équivalent à test_complet.js)
 * Drogon HTTP client, synchrone
 */

using namespace drogon;
using namespace std;

// ------------------- CONFIG -------------------
const string API_BASE_URL = "http://localhost:4100";
const string ADMIN_EMAIL  = "admin1@planteshop.com";
const string ADMIN_PASS   = "password";
map<string, string> cookieJar;
auto now = chrono::system_clock::to_time_t(chrono::system_clock::now());

// ------------------- HELPERS -------------------
string timestamp() {
	char buf[32];
	strftime(buf, sizeof(buf), "%Y%m%d%H%M%S", localtime(&now));
	return buf;
}

string getCookie(const string& who) {
	if (cookieJar.count(who)) return cookieJar[who];
	return "";
}

/**
 * Sauvegarde le cookie jwt en provenance de la réponse HTTP
 * Utilise d'abord l'API cookies de Drogon, puis fallback Set-Cookie brut.
 * @who profil (admin|user)
 * @res réponse HTTP reçue
 */
void saveCookie(const string& who, const HttpResponsePtr& res) {
	// 1) Tentative via l’API cookies de Drogon
	//    (certains clients ne remontent pas 'Set-Cookie' dans headers()).
	try {
		// Map<string,string> attendue: nom -> valeur
		const auto& cookies = res->cookies();
		auto iterator = cookies.find("jwt");
		if (iterator != cookies.end()) {
			const string pair = "jwt=" + iterator->second;
			cookieJar[who] = pair;
			cout << "🍪 Cookie enregistré pour " << who << ": " << pair << endl;
			return;
		}
	} catch (...) {
		// on ignore et passe au fallback
	}

	// 2) Fallback: lecture brute d’un éventuel Set-Cookie agrégé
	string cookieLine;

	// a) accesseur direct si disponible
	try {
		if (res->getHeader("set-cookie").size() > 0) {
			cookieLine = res->getHeader("set-cookie");
		}
	} catch (...) {
		// b) parcours des headers (certains builds l’exposent ici)
		for (const auto& header : res->headers()) {
			string key = header.first;
			for (char& c : key)
				c = static_cast<char>(tolower(static_cast<unsigned char>(c)));
			if (key == "set-cookie") {
				cookieLine = header.second;
				break;
			}
		}
	}

	if (!cookieLine.empty()) {
		// Prend uniquement "name=value" du premier cookie
		size_t firstComma = cookieLine.find(',');
		if (firstComma != string::npos)
			cookieLine = cookieLine.substr(0, firstComma);
		size_t pos = cookieLine.find(';');
		string val = (pos == string::npos) ? cookieLine : cookieLine.substr(0, pos);
		cookieJar[who] = val;
		cout << "🍪 Cookie enregistré pour " << who << ": " << val << endl;
		return;
	}

	cout << "⚠️ Aucun cookie trouvé pour " << who << endl;
}

void assertEq(const Json::Value& obj, const string& key, const Json::Value& expected) {
	if (!obj.isMember(key)) throw runtime_error("Clé absente: " + key);
	if (obj[key] != expected)
		throw runtime_error("Assertion échouée: " + key + " attendu " + expected.asString());
	cout << "✅ " << key << " = " << expected.asString() << endl;
}

void assertNumericId(const Json::Value& id, const string& label) {
	string s = id.isInt() ? to_string(id.asInt()) : id.asString();
	if (!regex_match(s, regex("^[0-9]+$")))
		throw runtime_error(label + " doit être un identifiant numérique");
}

void assertSortedAscByField(const Json::Value& arr, const string& field, const string& label) {
	if (!arr.isArray() || arr.size() < 2) return;
	for (Json::ArrayIndex i=1;i<arr.size();i++) {
		string a = arr[i-1][field].asString();
		string b = arr[i][field].asString();
		if (a > b) throw runtime_error("Liste " + label + " non triée croissant par " + field);
	}
}

void assertAdminsFirstThenName(const Json::Value& arr) {
	if (!arr.isArray() || arr.size() < 2) return;
	bool foundNonAdmin=false;
	for (Json::ArrayIndex i=0;i<arr.size();i++) {
		auto cur=arr[i];
		if (!cur.isMember("admin")) throw runtime_error("Objet user sans champ admin");
		if (foundNonAdmin && cur["admin"].asBool()) throw runtime_error("Admins doivent précéder non-admins");
		if (!cur["admin"].asBool()) foundNonAdmin=true;
		if (i>0 && cur["admin"]==arr[i-1]["admin"]) {
			string prev=arr[i-1]["name"].asString(), next=cur["name"].asString();
			if (prev>next) throw runtime_error("Tri alphabétique ascendant incorrect");
		}
	}
}

// ------------------- HTTP -------------------
Json::Value hit(const string& method, const string& route, int expected,
                const Json::Value* body=nullptr, const string& who="default") {
	static bool started = false;
	static trantor::EventLoopThread loopThread;

	// Démarre le thread d’événements au premier appel
	if (!started) {
		loopThread.run();
		std::this_thread::sleep_for(std::chrono::milliseconds(50)); // laisse le temps de lancer la boucle
		started = true;
	}

	auto loop = loopThread.getLoop();
	auto client = HttpClient::newHttpClient(API_BASE_URL, loop);


	auto req = HttpRequest::newHttpRequest();
	std::string fullPath = "/api" + (route[0] == '/' ? route : "/" + route);
	req->setPath(fullPath);
	req->setMethod(method=="POST"?Post:method=="PATCH"?Patch:method=="DELETE"?Delete:Get);
	req->setContentTypeCode(drogon::CT_APPLICATION_JSON);
	req->addHeader("Content-Type", "application/json");
	req->addHeader("Accept","application/json");
	req->addHeader("Connection", "close");
	const string cookie = getCookie(who);
	if (!cookie.empty()) {
		cout << "🔎 [DEBUG] Cookie utilisé pour " << who << ": " << cookie << endl;
		req->addHeader("Cookie", cookie);
	} else {
		cout << "🔎 [DEBUG] Aucun cookie envoyé pour " << who << endl;
	}
	if (body) {
		Json::StreamWriterBuilder b;
		b["indentation"]="";
		const string jsonBody = Json::writeString(b, *body);
		cout << "🔎 [DEBUG] JSON envoyé à " << route << ": " << jsonBody << endl;
		req->setBody(jsonBody);
		req->addHeader("Content-Length", std::to_string(jsonBody.size()));
	}

	std::promise<std::pair<ReqResult, HttpResponsePtr>> prom;
	auto fut = prom.get_future();

	req->addHeader("Host", "localhost:4100");
	req->addHeader("User-Agent", "drogon-client");
	req->addHeader("Connection", "keep-alive");

	// Forcer un Content-Type sans charset
	req->removeHeader("Content-Type");
	req->addHeader("Content-Type", "application/json");

	// Ne pas appeler setContentTypeCode ici
	client->sendRequest(req, [&prom](ReqResult resCode, const HttpResponsePtr& res) {
		prom.set_value({resCode, res});
	});

	auto [resCode, res] = fut.get();
	if (resCode != ReqResult::Ok) throw runtime_error("Erreur de connexion: " + route);
		if (route == "/auth/login") {
		cout << "🔎 [DEBUG] Login response code: " << res->getStatusCode() << endl;
		cout << "🔎 [DEBUG] Headers returned:" << endl;
		for (const auto& [key, val] : res->headers()) {
			cout << "   [" << key << "] => " << val << endl;
		}
		cout << "🔎 [DEBUG] All headers printed above" << endl;
		for (const auto& [key, val] : res->headers()) {
			cout << "   " << key << ": " << val << endl;
		}
		cout << "🔎 [DEBUG] Body: " << res->getBody() << endl;
	}
	cout << "🔎 [DEBUG] Body brut: [" << res->getBody() << "]" << endl;
	saveCookie(who, res);
	if (res->getStatusCode() != expected)
		throw runtime_error("API " + route + " → " + to_string(res->getStatusCode()) + " attendu " + to_string(expected));

	Json::CharReaderBuilder builder;
	Json::Value json;
	string errs;
	string responseBody(res->getBody());
	std::istringstream s(responseBody);
	Json::parseFromStream(builder, s, &json, &errs);
	return json;
}

// ------------------- MODULES -------------------
/**
 * Connexion utilisateur : vérifie les champs avant envoi.
 */
void login(const string& email,const string& password,const string& who) {
	if (email.empty() || password.empty())
		throw runtime_error("login(): email ou password vide (" + who + ")");
	Json::Value creds;
	creds["email"] = email;
	creds["password"] = password;
	cout << "🔎 [DEBUG] Tentative login pour " << who
	     << " email=" << email << endl;
	hit("POST","/auth/login",201,&creds,who);
}

void registerUser(const string& name,const string& email,const string& pass,const string& who) {
	Json::Value user; user["name"]=name; user["email"]=email; user["password"]=pass;
	hit("POST","/auth/register",201,&user,who);
}

Json::Value findUserIdByEmail(const string& who,const string& email) {
	auto users=hit("GET","/users",200,nullptr,who);
	for (const auto& u:users) if (u["email"].asString()==email) return u["id"];
	throw runtime_error("User "+email+" introuvable");
}

void testPlants(const string& who="admin") {
	cout<<"\n📌 TEST MODULE: PLANTS\n";
	Json::Value plant; plant["name"]="Test Plant"; plant["price"]=10; plant["stock"]=5;
	auto created=hit("POST","/admin/plants",201,&plant,who);
	assertNumericId(created["id"],"plantId");
	int pid=created["id"].asInt();

	assertEq(hit("GET","/plants/"+to_string(pid),200,nullptr,who),"name",plant["name"]);
	Json::Value upd; upd["price"]=15;
	hit("PATCH","/admin/plants/"+to_string(pid),200,&upd,who);
	assertEq(hit("GET","/plants/"+to_string(pid),200,nullptr,who),"price",15);
	hit("DELETE","/admin/plants/"+to_string(pid),200,nullptr,who);
}

void testUsers(const string& who="admin") {
	cout<<"\n📌 TEST MODULE: USERS\n";
	Json::Value user; user["email"]="utilisateur_test_"+timestamp()+"@example.com";
	user["name"]="Utilisateur Test"; user["password"]="pass123";
	auto created=hit("POST","/users",201,&user,who);
	int uid=created["id"].asInt();

	Json::Value upd; upd["name"]="Tester Update";
	hit("PATCH","/users/"+to_string(uid),200,&upd,who);
	assertEq(hit("GET","/users/"+to_string(uid),200,nullptr,who),"name","Tester Update");
	hit("DELETE","/users/"+to_string(uid),200,nullptr,who);
}

void testOrders(const string& adminWho,const string& userWho) {
	cout << "\n📌 TEST MODULE: ORDERS & ORDER ITEMS\n";

	Json::Value plant;
	plant["name"] = "Plante_de_test_" + timestamp();
	plant["price"] = 10;
	plant["stock"] = 5;
	auto p = hit("POST", "/admin/plants", 201, &plant, adminWho);
	int plantId = p["id"].asInt();

	Json::Value order;
	Json::Value item;
	item["plantId"] = plantId;
	item["quantity"] = 2;
	order["items"].append(item);
	auto o = hit("POST", "/orders", 201, &order, userWho);
	int orderId = o["id"].asInt();

	Json::Value status;
	status["status"] = "shipped";
	hit("PATCH", "/orders/" + to_string(orderId), 200, &status, adminWho);

	auto orders = hit("GET", "/orders", 200, nullptr, userWho);
	for (auto& cmd : orders) if (cmd["id"] == orderId) {
		assertEq(cmd, "status", "shipped");

		// --- Vérifications renforcées ---
		if (!cmd.isMember("orderItems") || !cmd["orderItems"].isArray() || cmd["orderItems"].empty())
			throw runtime_error("orderItems manquant ou vide");

		for (const auto& it : cmd["orderItems"]) {
			if (!it.isMember("plant") || !it["plant"].isObject())
				throw runtime_error("orderItems[].plant manquant ou invalide");
			if (!it.isMember("quantity") || !it["quantity"].isInt())
				throw runtime_error("orderItems[].quantity manquant ou invalide");
		}

		assertEq(cmd["orderItems"][0]["plant"], "name", plant["name"]);
	}

	hit("DELETE", "/orders/" + to_string(orderId), 200, nullptr, adminWho);
	hit("DELETE", "/admin/plants/" + to_string(plantId), 200, nullptr, adminWho);
}

void testUserProfile(const string& adminWho,const string& userWho,const string& userEmail) {
	cout<<"\n📌 TEST MODULE: USER PROFILE\n";
	int uid=findUserIdByEmail(adminWho,userEmail).asInt();
	assertEq(hit("GET","/users/"+to_string(uid),200,nullptr,userWho),"id",uid);
	Json::Value upd; upd["name"]="Utilisateur_de_test_"+timestamp();
	hit("PATCH","/users/"+to_string(uid),200,&upd,userWho);
	assertEq(hit("GET","/users/"+to_string(uid),200,nullptr,userWho),"name",upd["name"]);
	Json::Value elev; elev["admin"]=true;
	hit("PATCH","/users/"+to_string(uid),200,&elev,userWho);
	assertEq(hit("GET","/users/"+to_string(uid),200,nullptr,adminWho),"admin",false);
}

void testAuthRoles(const string& adminWho,const string& userWho) {
	cout<<"\n📌 TEST MODULE: ROLES\n";
	Json::Value bad; bad["name"]="Bad"; bad["price"]=1; bad["stock"]=1;
	hit("POST","/admin/plants",403,&bad,userWho);
	Json::Value good; good["name"]="Good"; good["price"]=1; good["stock"]=1;
	auto p=hit("POST","/admin/plants",201,&good,adminWho);
	int pid=p["id"].asInt();
	hit("DELETE","/admin/plants/"+to_string(pid),200,nullptr,adminWho);
	hit("GET","/users",403,nullptr,userWho);
}

void testAdminPlants(const string& who="admin") {
	cout<<"\n📌 TEST MODULE: ADMIN PLANTS\n";
	auto plantes=hit("GET","/admin/plants",200,nullptr,who);
	assertSortedAscByField(plantes,"name","plantes");
	Json::Value d; d["name"]="Plante_admin_de_test_"+timestamp(); d["price"]=99; d["stock"]=12;
	auto c=hit("POST","/admin/plants",201,&d,who);
	int id=c["id"].asInt();
	Json::Value upd; upd["price"]=123;
	hit("PATCH","/admin/plants/"+to_string(id),200,&upd,who);
	hit("DELETE","/admin/plants/"+to_string(id),200,nullptr,who);
}

void testAdminUsers(const string& who="admin") {
	cout<<"\n📌 TEST MODULE: ADMIN USERS\n";
	Json::Value admin; admin["email"]="admin_temp_"+timestamp()+"@example.com";
	admin["name"]="Admin Temporaire "+timestamp(); admin["password"]="password"; admin["admin"]=true;
	auto c=hit("POST","/users",201,&admin,who);
	auto list=hit("GET","/admin/users",200,nullptr,who);
	bool found=false; int id=-1;
	for (auto& a:list) if (a["email"]==admin["email"]) {found=true; id=a["id"].asInt();}
	if (!found) throw runtime_error("Admin temp non trouvé");
	Json::Value upd; upd["name"]="Admin_temp_modifié_"+timestamp();
	hit("PATCH","/users/"+to_string(id),200,&upd,who);
	assertEq(hit("GET","/users/"+to_string(id),200,nullptr,who),"name",upd["name"]);
	hit("DELETE","/users/"+to_string(id),200,nullptr,who);
}

void testAuthMe(const string& who="user") {
	cout<<"\n📌 TEST MODULE: AUTH /me\n";
	auto me=hit("GET","/auth/me",200,nullptr,who);
	if (!me.isMember("email")) throw runtime_error("/auth/me invalide");
	cout<<"   ↳ Utilisateur connecté: "<<me["email"].asString()<<" ("<<me["name"].asString()<<")"<<endl;
}

// ------------------- MAIN -------------------
// int main() {
// 	try {
// 		cout<<"🧪 Démarrage des tests: "<<API_BASE_URL<<"\n";
// 		login(ADMIN_EMAIL,ADMIN_PASS,"admin");
// 		cout << "🔍 Cookie admin après login: " << getCookie("admin") << endl;
// 		string userEmail="utilisateur_de_test_"+timestamp()+"@example.com";
// 		registerUser("User",userEmail,"pass123","user");
// 		login(userEmail,"pass123","user");

// 		testPlants("admin");
// 		testUsers("admin");
// 		testOrders("admin","user");
// 		testUserProfile("admin","user",userEmail);
// 		testAuthRoles("admin","user");
// 		testAdminPlants("admin");
// 		testAdminUsers("admin");
// 		testAuthMe("user");

// 		cout<<"\n🎉 Tous les tests ont réussi !\n";
// 		return 0;
// 	} catch(const exception& e) {
// 		cerr<<"\n❌ Tests interrompus: "<<e.what()<<endl;
// 		return 1;
// 	}
// }



int main() {
	try {
		cout << "🧪 Test LOGIN seul\n";
		login(ADMIN_EMAIL, ADMIN_PASS, "admin");
		cout << "✅ Login réussi\n";
		return 0;
	} catch (const exception& e) {
		cerr << "❌ Erreur LOGIN: " << e.what() << endl;
		return 1;
	}
}
