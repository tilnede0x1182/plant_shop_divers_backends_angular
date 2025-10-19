use std::env;
use dotenvy::dotenv;
use sea_orm::DatabaseConnection;
use poem::{
	listener::TcpListener,
	Route,
	Server,
	get,
	post,
	patch,
	http::Method,
	middleware::{AddData, Cors},
	EndpointExt,
};

mod errors;
mod db;
mod auth;
mod users;
mod plants;
mod orders;
mod order_items;

use crate::{
	auth::handlers::{login, register, me, logout},
	db::migrations::run_migrations,
	db::connect_db,
	order_items::handlers::{get_order_item, update_order_item, delete_order_item},
	orders::handlers::{create_order, list_orders, get_order, update_order, delete_order},
	plants::handlers::{create_plant, list_plants, get_plant, update_plant, delete_plant},
	users::handlers::{get_user, update_user, delete_user, list_users, create_user},
};

#[tokio::main]
async fn main() -> Result<(), std::io::Error> {
	// Charger .env et config
	dotenv().ok();

	// Connexion via SeaORM
	let db: DatabaseConnection = connect_db().await;

	// Migration SeaORM
	if let Err(e) = run_migrations(&db).await {
		eprintln!("Erreur lors de l'application des migrations: {}", e);
	}

	let cors = Cors::new()
		.allow_credentials(true)
		.allow_origin("http://localhost:8300")
		.allow_methods(vec![
			Method::GET,
			Method::POST,
			Method::PUT,
			Method::DELETE,
			Method::PATCH,
		]);

	// Définir toutes les routes REST
	let app = Route::new()
		// /api/auth/*
		.nest(
			"/api/auth",
			Route::new()
				.at("/login", post(login))
				.at("/register", post(register))
				.at("/me", get(me))
				.at("/logout", post(logout)),
		)
		// /api/admin/plants (GET liste, POST create, PATCH/DELETE by id)
		.nest(
			"/api/admin/plants",
			Route::new()
				.at("/", get(list_plants).post(create_plant))
				.at("/:id", patch(update_plant).delete(delete_plant)),
		)
		// /api/admin/users (GET liste admin, PATCH/DELETE by id)
		.nest(
			"/api/admin/users",
			Route::new()
				.at("/", get(list_users))
				.at("/:id", patch(update_user).delete(delete_user)),
		)
		// /api/users (POST create via admin, GET liste pour admin) + /api/users/:id
		.nest(
			"/api/users",
			Route::new()
				.at("/", post(create_user).get(list_users))
				.at("/:id", get(get_user).patch(update_user).delete(delete_user)),
		)
		// /api/plants (public GET) + /api/plants/:id
		.nest(
			"/api/plants",
			Route::new()
				.at("/", get(list_plants))
				.at("/:id", get(get_plant)),
		)
		// /api/orders (user GET liste, POST create) + /api/orders/:id (GET/PATCH/DELETE)
		.nest(
			"/api/orders",
			Route::new()
				.at("/", get(list_orders).post(create_order))
				.at("/:id", get(get_order).patch(update_order).delete(delete_order)),
		)
		// /api/order_items/:id (GET/PATCH/DELETE)
		.nest(
			"/api/order_items",
			Route::new()
				.at("/:id", get(get_order_item).patch(update_order_item).delete(delete_order_item)),
		)
		.with(AddData::new(db))
		.with(poem::middleware::CookieJarManager::new())
		.with(cors);

	// Lancer serveur HTTP sur le port 4100
	println!("🚀 Serveur démarré sur http://0.0.0.0:4100");
	Server::new(TcpListener::bind("0.0.0.0:4100"))
		.run(app)
		.await
}
