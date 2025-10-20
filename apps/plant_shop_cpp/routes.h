#pragma once
#include <drogon/drogon.h>
#include "controllers/AuthController.h"
#include "controllers/UserController.h"
#include "controllers/PlantController.h"
#include "controllers/OrderController.h"

/**
 * """ Configuration complète des routes HTTP du backend Plant Shop
 */

inline void registerRoutes() {
	using namespace drogon;

	// ───────────────────────────────
	//   AUTHENTIFICATION
	// ───────────────────────────────
	app().registerHandler("/api/auth/register",
	                      &AuthController::registerUser,
	                      {Post});
	app().registerHandler("/api/auth/login",
	                      &AuthController::login,
	                      {Post});
	app().registerHandler("/api/auth/logout",
	                      &AuthController::logout,
	                      {Post});
	app().registerHandler("/api/auth/me",
	                      &AuthController::me,
	                      {Get});

	// ───────────────────────────────
	//   USERS
	// ───────────────────────────────
	app().registerHandler("/api/users",
	                      &UserController::createUser,
	                      {Post});
	app().registerHandler("/api/users",
	                      &UserController::listUsers,
	                      {Get});
	app().registerHandler("/api/users/{1}",
	                      &UserController::getUser,
	                      {Get});
	app().registerHandler("/api/users/{1}",
	                      &UserController::updateUser,
	                      {Patch});
	app().registerHandler("/api/users/{1}",
	                      &UserController::deleteUser,
	                      {Delete});

	// ───────────────────────────────
	//   ADMIN USERS
	// ───────────────────────────────
	app().registerHandler("/api/admin/users",
	                      &UserController::listAdminUsers,
	                      {Get});
	app().registerHandler("/api/admin/users/{1}",
	                      &UserController::updateAdminUser,
	                      {Patch});
	app().registerHandler("/api/admin/users/{1}",
	                      &UserController::deleteAdminUser,
	                      {Delete});

	// ───────────────────────────────
	//   PLANTS
	// ───────────────────────────────
	app().registerHandler("/api/plants",
	                      &PlantController::listPlants,
	                      {Get});
	app().registerHandler("/api/plants/{1}",
	                      &PlantController::getPlant,
	                      {Get});

	// ───────────────────────────────
	//   ADMIN PLANTS
	// ───────────────────────────────
	app().registerHandler("/api/admin/plants",
	                      &PlantController::createPlant,
	                      {Post});
	app().registerHandler("/api/admin/plants",
	                      &PlantController::listAdminPlants,
	                      {Get});
	app().registerHandler("/api/admin/plants/{1}",
	                      &PlantController::updatePlant,
	                      {Patch});
	app().registerHandler("/api/admin/plants/{1}",
	                      &PlantController::deletePlant,
	                      {Delete});

	// ───────────────────────────────
	//   ORDERS
	// ───────────────────────────────
	app().registerHandler("/api/orders",
	                      &OrderController::createOrder,
	                      {Post});
	app().registerHandler("/api/orders",
	                      &OrderController::listOrders,
	                      {Get});
	app().registerHandler("/api/orders/{1}",
	                      &OrderController::getOrder,
	                      {Get});
	app().registerHandler("/api/orders/{1}",
	                      &OrderController::updateOrder,
	                      {Patch});
	app().registerHandler("/api/orders/{1}",
	                      &OrderController::deleteOrder,
	                      {Delete});

	// ───────────────────────────────
	//   ORDER ITEMS (optionnel, pour cohérence Rust)
	// ───────────────────────────────
	app().registerHandler("/api/order_items/{1}",
	                      &OrderController::getOrderItem,
	                      {Get});
	app().registerHandler("/api/order_items/{1}",
	                      &OrderController::updateOrderItem,
	                      {Patch});
	app().registerHandler("/api/order_items/{1}",
	                      &OrderController::deleteOrderItem,
	                      {Delete});

	// ───────────────────────────────
	//   LOGS
	// ───────────────────────────────
	LOG_INFO << "✅ Routes enregistrées : /auth, /users, /plants, /orders, /admin/*";
}
