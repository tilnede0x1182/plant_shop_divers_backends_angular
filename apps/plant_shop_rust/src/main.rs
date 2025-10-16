use poem::{listener::TcpListener, Route, Server, middleware::AddData, web::Data, EndpointExt};
use poem::{get, post, patch, put, delete};
use sqlx::postgres::PgPoolOptions;
use dotenvy::dotenv;
use std::env;
use std::sync::Arc;

mod config;
mod errors;
mod db;
mod auth;
mod users;
mod plants;
mod orders;
mod order_items;

use db::migrations::run_migrations;
use auth::handlers::{login, register, me, logout};
use users::handlers::{get_user, update_user, delete_user};
use plants::handlers::{create_plant, list_plants, get_plant, update_plant, delete_plant};
use orders::handlers::{create_order, list_orders, get_order, update_order, delete_order};
use order_items::handlers::{get_order_item, update_order_item, delete_order_item};

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
	run_migrations(&pool).await.expect("Échec migration");

	let shared_db = Arc::new(pool);

	// Définir toutes les routes REST
	let app = Route::new()
		.nest("/api/auth",
			Route::new()
				.at("/login", post(login))
				.at("/register", post(register))
				.at("/me", get(me))
				.at("/logout", post(logout))
		)
		.nest("/api/users",
			Route::new()
				.at("/:id", get(get_user))
				.at("/:id", put(update_user))
				.at("/:id", delete(delete_user))
		)
		.nest("/api/plants",
			Route::new()
				.at("/", post(create_plant))
				.at("/", get(list_plants))
				.at("/:id", get(get_plant))
				.at("/:id", patch(update_plant))
				.at("/:id", delete(delete_plant))
		)
		.nest("/api/orders",
			Route::new()
				.at("/", post(create_order))
				.at("/", get(list_orders))
				.at("/:id", get(get_order))
				.at("/:id", patch(update_order))
				.at("/:id", delete(delete_order))
		)
		.nest("/api/order_items",
			Route::new()
				.at("/:id", get(get_order_item))
				.at("/:id", patch(update_order_item))
				.at("/:id", delete(delete_order_item))
		)
		.with(AddData::new(shared_db.clone()));

	// Lancer serveur HTTP
	Server::new(TcpListener::bind("0.0.0.0:3000"))
		.run(app)
		.await
}
