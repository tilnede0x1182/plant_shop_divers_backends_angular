use poem::{listener::TcpListener, Route, Server, middleware::AddData, web::Data, EndpointExt};
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
				.post("/login", login)
				.post("/register", register)
				.get("/me", me)
				.post("/logout", logout)
		)
		.nest("/api/users",
			Route::new()
				.get("/:id", get_user)
				.put("/:id", update_user)
				.delete("/:id", delete_user)
		)
		.nest("/api/plants",
			Route::new()
				.post("/", create_plant)
				.get("/", list_plants)
				.get("/:id", get_plant)
				.patch("/:id", update_plant)
				.delete("/:id", delete_plant)
		)
		.nest("/api/orders",
			Route::new()
				.post("/", create_order)
				.get("/", list_orders)
				.get("/:id", get_order)
				.patch("/:id", update_order)
				.delete("/:id", delete_order)
		)
		.nest("/api/order_items",
			Route::new()
				.get("/:id", get_order_item)
				.patch("/:id", update_order_item)
				.delete("/:id", delete_order_item)
		)
		.with(AddData::new(Data::new(shared_db)));

	// Lancer serveur HTTP
	Server::new(TcpListener::bind("0.0.0.0:3000"))
		.run(app)
		.await
}
