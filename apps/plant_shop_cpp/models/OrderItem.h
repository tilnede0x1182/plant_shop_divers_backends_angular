#pragma once
#include <json/json.h>

/**
 * """ Modèle d’élément de commande (OrderItem)
 *   Correspond à la table SQL 'order_items'
 *   - id SERIAL PRIMARY KEY
 *   - order_id INTEGER
 *   - plant_id INTEGER
 *   - quantity INTEGER
 *   - price NUMERIC(10,2)
 * """
 */
struct OrderItem {
	int id;
	int order_id;
	int plant_id;
	int quantity;
	double price;
	std::string plant_name; // ajouté pour affichage JSON test

	/** Conversion en JSON (structure test_complet.js) */
	Json::Value toJson() const {
		Json::Value j;
		j["id"] = id;
		j["plantId"] = plant_id;
		j["quantity"] = quantity;
		j["price"] = price;
		Json::Value plant;
		plant["id"] = plant_id;
		plant["name"] = plant_name;
		j["plant"] = plant;
		return j;
	}
};
