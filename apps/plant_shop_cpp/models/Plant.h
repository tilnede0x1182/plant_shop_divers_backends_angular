#pragma once
#include <string>
#include <json/json.h>

/**
 * """ Modèle Plant
 *   Table SQL: plants
 *   Champs:
 *     id SERIAL PRIMARY KEY
 *     name VARCHAR(255)
 *     description TEXT
 *     price NUMERIC(10,2)
 *     stock INTEGER
 *     created_at TIMESTAMPTZ DEFAULT NOW()
 * """
 */
struct Plant {
	int id{};
	std::string name;
	std::string description;
	double price{};
	int stock{};
	std::string created_at;

	/** """ Sérialisation JSON conforme au test JS """ */
	Json::Value toJson() const {
		Json::Value j;
		j["id"] = id;
		j["name"] = name;
		j["description"] = description;
		j["price"] = price;
		j["stock"] = stock;
		j["created_at"] = created_at;
		return j;
	}

	/** """ Construction depuis JSON pour POST / PATCH """ */
	static Plant fromJson(const Json::Value& data) {
		Plant p;
		if (data.isMember("id")) p.id = data["id"].asInt();
		if (data.isMember("name")) p.name = data["name"].asString();
		if (data.isMember("description")) p.description = data["description"].asString();
		if (data.isMember("price")) p.price = data["price"].asDouble();
		if (data.isMember("stock")) p.stock = data["stock"].asInt();
		if (data.isMember("created_at")) p.created_at = data["created_at"].asString();
		return p;
	}
};
