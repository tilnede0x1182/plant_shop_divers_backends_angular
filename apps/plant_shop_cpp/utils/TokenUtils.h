#pragma once
#include <string>

/** """ Génère un token base64(email|id|name|admin|exp|sig) """ */
std::string generateToken(const std::string& email, int64_t userId, const std::string& name, bool admin);

/** """ Décode et valide le token reçu """ */
bool parseToken(const std::string& token, std::string& email, int64_t& userId, std::string& name, bool& admin);
