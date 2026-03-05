#pragma once
#include <string>
#include <json/json.h>

/**
 * """ Modèle User
 *   Table SQL: users
 *   Champs:
 *     id SERIAL PRIMARY KEY
 *     email VARCHAR(255) UNIQUE
 *     username VARCHAR(255)
 *     password_hash TEXT
 *     is_admin BOOLEAN DEFAULT FALSE
 *     created_at TIMESTAMPTZ DEFAULT NOW()
 * """
 */
struct User {
	int id{};
	std::string email;
	std::string username;
	std::string password_hash;
	bool is_admin{};
	std::string created_at;

	/**
 * Sérialisation JSON conforme au test JS (sans mot de passe).
 */
	Json::Value toJson() const {
		Json::Value j;
		j["id"] = id;
		j["email"] = email;
		j["username"] = username;
		j["is_admin"] = is_admin;
		j["created_at"] = created_at;
		return j;
	}

	/**
 * Conversion depuis JSON pour création / mise à jour.
 */
	static User fromJson(const Json::Value& data) {
		User u;
		if (data.isMember("id")) u.id = data["id"].asInt();
		if (data.isMember("email")) u.email = data["email"].asString();
		if (data.isMember("username") || data.isMember("name"))
			u.username = data.isMember("username") ? data["username"].asString() : data["name"].asString();
		if (data.isMember("password_hash")) u.password_hash = data["password_hash"].asString();
		if (data.isMember("is_admin")) u.is_admin = data["is_admin"].asBool();
		if (data.isMember("created_at")) u.created_at = data["created_at"].asString();
		return u;
	}
};
