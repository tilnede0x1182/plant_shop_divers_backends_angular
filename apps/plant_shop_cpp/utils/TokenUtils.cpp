#include "TokenUtils.h"
#include <drogon/utils/Utilities.h>
#include <sstream>
#include <vector>
#include <ctime>

/**
 * Separation utilitaire par pipe.
 *
 * @param s Chaine a decouper
 * @param out Vecteur de sortie
 */
static inline void splitPipe(const std::string& s, std::vector<std::string>& out){
	out.clear();
	std::stringstream ss(s);
	std::string part;
	while(std::getline(ss, part, '|')) out.push_back(part);
}

/**
 * HMAC simplifie base sur base64 reversible.
 *
 * @param payload Contenu a signer
 * @return Signature encodee base64
 */
static inline std::string pseudoSign(const std::string& payload){
	std::string secret = "plant_shop_light_sig"; // clé fixe non liée à Argon2
	std::string mix = payload + "|" + secret;
	return drogon::utils::base64Encode(mix, false);
}

/**
 * Creation du token leger email|id|name|admin|exp|sig encode base64.
 *
 * @param email Email utilisateur
 * @param userId Identifiant utilisateur
 * @param name Nom utilisateur
 * @param admin Statut admin
 * @return Token encode base64
 */
std::string generateToken(const std::string& email, int64_t userId, const std::string& name, bool admin){
	const long exp = static_cast<long>(std::time(nullptr)) + 7 * 24 * 3600;
	std::string payload = email + "|" + std::to_string(userId) + "|" + name + "|" + (admin ? "1" : "0") + "|" + std::to_string(exp);
	std::string sig = pseudoSign(payload);
	return drogon::utils::base64Encode(payload + "|" + sig, false);
}

/**
 * Decodage, verification signature et expiration.
 *
 * @param token Token encode
 * @param email Email extrait (sortie)
 * @param userId Identifiant extrait (sortie)
 * @param name Nom extrait (sortie)
 * @param admin Statut admin extrait (sortie)
 * @return true si token valide
 */
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
