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
		.nest("/api/auth",
			Route::new()
				.at("/login", post(login))
				.at("/register", post(register))
				.at("/me", get(me))
				.at("/logout", post(logout))
		)
        .nest("/api/admin",
            Route::new()
                .nest("/plants",
                    Route::new()
                        .at("/", get(list_plants))
                        .at("/", post(create_plant))
                        .at("/:id", patch(update_plant))
                        .at("/:id", delete(delete_plant))
                )
                .nest("/users",
                    Route::new()
                        .at("/", get(list_users))
                        .at("/:id", patch(update_user))
                )
        )
		.nest("/api/users",
			Route::new()
				.at("/", post(create_user)) // Le test crée un user via /users, pas /auth/register
				.at("/:id", get(get_user))
				.at("/:id", patch(update_user))
				.at("/:id", delete(delete_user))
		)
		.nest("/api/plants",
			Route::new()
				.at("/", get(list_plants))
				.at("/:id", get(get_plant))
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
		.with(AddData::new(pool))
        .with(cors);

	// Lancer serveur HTTP sur le port 4100
	println!("🚀 Serveur démarré sur http://0.0.0.0:4100");
	Server::new(TcpListener::bind("0.0.0.0:4100"))
		.run(app)
		.await
}
