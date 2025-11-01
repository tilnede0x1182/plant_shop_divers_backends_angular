#pragma once
#include <string>
#include <vector>
#include <json/json.h>
#include "OrderItem.h"

/**
 * """ Modèle de commande (Order)
 *   Correspond à la table SQL 'orders'
 *   - id SERIAL PRIMARY KEY
 *   - user_id INTEGER (nullable)
 *   - total NUMERIC(10,2)
 *   - status VARCHAR(50)
 *   - created_at TIMESTAMPTZ
 * """
 */
struct Order {
	int id;
	int user_id;
	double total;
	std::string status;
	std::string created_at;
	std::vector<OrderItem> items;

	/** Conversion en JSON pour l’API */
	Json::Value toJson() const {
		Json::Value j;
		j["id"] = id;
		j["status"] = status;
		j["totalPrice"] = total;
		j["createdAt"] = created_at;

		Json::Value arr(Json::arrayValue);
		for (const auto& it : items) arr.append(it.toJson());
		j["orderItems"] = arr;

		return j;
	}
};
