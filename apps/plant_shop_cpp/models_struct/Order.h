#pragma once
#include <string>
#include <vector>
#include <json/json.h>
#include "OrderItem.h"

/**
 * Modele de commande (Order).
 * Correspond a la table SQL orders.
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
