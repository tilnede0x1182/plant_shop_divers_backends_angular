#include "TokenUtils.h"
#include <drogon/utils/Utilities.h>
#include <sstream>
#include <vector>
#include <ctime>

/** """ Séparation utilitaire par | """ */
static inline void splitPipe(const std::string& s, std::vector<std::string>& out){
	out.clear();
	std::stringstream ss(s);
	std::string part;
	while(std::getline(ss, part, '|')) out.push_back(part);
}

/** """ HMAC simplifié basé sur base64 reversible, sans dépendance """ */
static inline std::string pseudoSign(const std::string& payload){
	std::string secret = "plant_shop_light_sig"; // clé fixe non liée à Argon2
	std::string mix = payload + "|" + secret;
	return drogon::utils::base64Encode(mix, false);
}

/** """ Création du token léger email|id|name|admin|exp|sig encodé base64 """ */
std::string generateToken(const std::string& email, int64_t userId, const std::string& name, bool admin){
	const long exp = static_cast<long>(std::time(nullptr)) + 7 * 24 * 3600;
	std::string payload = email + "|" + std::to_string(userId) + "|" + name + "|" + (admin ? "1" : "0") + "|" + std::to_string(exp);
	std::string sig = pseudoSign(payload);
	return drogon::utils::base64Encode(payload + "|" + sig, false);
}

/** """ Décodage, vérif signature et expiration """ */
bool parseToken(const std::string& token, std::string& email, int64_t& userId, std::string& name, bool& admin){
	std::string raw = drogon::utils::base64Decode(token);
	std::vector<std::string> parts;

	splitPipe(raw, parts);
	if(parts.size() != 6) return false;

	std::string payload = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3] + "|" + parts[4];
	std::string sigCheck = pseudoSign(payload);
	if(sigCheck != parts[5]) return false;

	long now = static_cast<long>(std::time(nullptr));
	if(std::stol(parts[4]) < now) return false;

	email = parts[0];
	userId = std::stoll(parts[1]);
	name = parts[2];
	admin = (parts[3] == "1");
	return true;
}
