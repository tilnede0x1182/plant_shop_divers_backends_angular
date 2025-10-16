use poem::{listener::TcpListener, Route, Server, middleware::{AddData, Cors}, EndpointExt};
use poem::{get, post, patch, delete};
use sqlx::postgres::PgPoolOptions;
use dotenvy::dotenv;
use std::env;

mod config;
mod errors;
mod db;
mod auth;
mod users;
mod plants;
mod orders;
mod order_items;

use crate::auth::handlers::{login, register, me, logout};
use crate::db::migrations::run_migrations;
use crate::order_items::handlers::{get_order_item, update_order_item, delete_order_item};
use crate::orders::handlers::{create_order, list_orders, get_order, update_order, delete_order};
use crate::plants::handlers::{create_plant, list_plants, get_plant, update_plant, delete_plant};
use crate::users::handlers::{get_user, update_user, delete_user, list_users, create_user};

#[tokio::main]
async fn main() -> Result<(), std::io::Error> {
	// Charger .env et config
	dotenv().ok();
	let database_url = env::var("DATABASE_URL").expect("DATABASE_URL manquant");
	let pool = PgPoolOptions::new()
		.max_connections(5)
		.connect(&database_url)
		.await
		.expect("Connexion base de données impossible");

	// Migration (SQLx)
	if let Err(e) = run_migrations(&pool).await {
        eprintln!("Erreur lors de l'application des migrations: {}", e);
    }

    let cors = Cors::new();

	// Définir toutes les routes REST
	let app = Route::new()
		// /api/auth/*
		.nest("/api/auth",
			Route::new()
				.at("/login", post(login))
				.at("/register", post(register))
				.at("/me", get(me))
				.at("/logout", post(logout))
		)
		// /api/admin/plants (GET liste, POST create, PATCH/DELETE by id)
		.nest("/api/admin/plants",
			Route::new()
				.at("/", get(list_plants).post(create_plant))
				.at("/:id", patch(update_plant).delete(delete_plant))
		)
		// /api/admin/users (GET liste admin, PATCH by id)
		.nest("/api/admin/users",
			Route::new()
				.at("/", get(list_users))
				.at("/:id", patch(update_user))
		)
		// /api/users (POST create via admin, GET liste pour admin) + /api/users/:id
		.nest("/api/users",
			Route::new()
				.at("/", post(create_user).get(list_users))
				.at("/:id", get(get_user).patch(update_user).delete(delete_user))
		)
		// /api/plants (public GET) + /api/plants/:id
		.nest("/api/plants",
			Route::new()
				.at("/", get(list_plants))
				.at("/:id", get(get_plant))
		)
		// /api/orders (user GET liste, POST create) + /api/orders/:id (GET/PATCH/DELETE)
		.nest("/api/orders",
			Route::new()
				.at("/", get(list_orders).post(create_order))
				.at("/:id", get(get_order).patch(update_order).delete(delete_order))
		)
		// /api/order_items/:id (GET/PATCH/DELETE)
		.nest("/api/order_items",
			Route::new()
				.at("/:id", get(get_order_item).patch(update_order_item).delete(delete_order_item))
		)
		.with(AddData::new(pool))
		.with(poem::middleware::CookieJarManager::new())
		.with(cors);


	// Lancer serveur HTTP sur le port 4100
	println!("🚀 Serveur démarré sur http://0.0.0.0:4100");
	Server::new(TcpListener::bind("0.0.0.0:4100"))
		.run(app)
		.await
}
