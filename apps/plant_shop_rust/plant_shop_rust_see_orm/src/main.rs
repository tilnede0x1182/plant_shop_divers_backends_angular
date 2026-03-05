//! Point d'entree du serveur Poem.

// ==============================================================================
// Importations
// ==============================================================================

use dotenvy::dotenv;
use poem::{
    get,
    http::Method,
    listener::TcpListener,
    middleware::{AddData, Cors},
    patch, post, EndpointExt, Route, Server,
};
use sea_orm::DatabaseConnection;
use std::net::TcpListener as StdTcpListener;

// ==============================================================================
// Constantes
// ==============================================================================

const PORT: u16 = 4100;

// ==============================================================================
// Modules
// ==============================================================================

mod auth;
mod config;
mod db;
mod entity;
mod errors;
mod order_items;
mod orders;
mod plants;
mod users;

use crate::{
    auth::handlers::{login, logout, me, register},
    db::connect_db,
    db::migrations::run_migrations,
    order_items::handlers::{delete_order_item, get_order_item, update_order_item},
    orders::handlers::{create_order, delete_order, get_order, list_orders, update_order},
    plants::handlers::{create_plant, delete_plant, get_plant, list_plants, update_plant},
    users::handlers::{create_user, delete_user, get_user, list_users, update_user},
};

// ==============================================================================
// Main
// ==============================================================================

/// Point d'entree principal du serveur Poem.
///
/// Configure la base de donnees, les migrations, CORS et les routes REST.
///
/// @return Ok(()) si le serveur s'arrete proprement, Err sinon
/// Verifie si un port est disponible.
///
/// @param port Numero de port a verifier
/// @return true si disponible, false sinon
fn is_port_available(port: u16) -> bool {
    StdTcpListener::bind(("0.0.0.0", port)).is_ok()
}

#[tokio::main]
async fn main() -> Result<(), std::io::Error> {
    if !is_port_available(PORT) {
        eprintln!("❌ Le port {} est déjà utilisé.", PORT);
        std::process::exit(1);
    }

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
                .at(
                    "/:id",
                    get(get_order).patch(update_order).delete(delete_order),
                ),
        )
        // /api/order_items/:id (GET/PATCH/DELETE)
        .nest(
            "/api/order_items",
            Route::new().at(
                "/:id",
                get(get_order_item)
                    .patch(update_order_item)
                    .delete(delete_order_item),
            ),
        )
        .with(AddData::new(db))
        .with(poem::middleware::CookieJarManager::new())
        .with(cors);

    // Lancer serveur HTTP
    println!("🚀 Serveur démarré sur http://0.0.0.0:{}", PORT);
    Server::new(TcpListener::bind(format!("0.0.0.0:{}", PORT)))
        .run(app)
        .await
}
