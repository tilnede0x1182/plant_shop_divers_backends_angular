use dotenvy::dotenv;
use sqlx::postgres::PgPoolOptions;
use std::env;
use tokio::runtime::Builder;

use poem::{
    get,
    http::Method,
    listener::TcpListener,
    middleware::{AddData, Cors},
    patch, post, EndpointExt, Route, Server,
};

mod auth;
mod cache;
mod db;
mod dto;
mod errors;
mod logging;
mod order_items;
mod orders;
mod plants;
mod response;
mod state;
mod users;

use crate::{
    auth::handlers::{login, logout, me, register},
    db::migrations::run_migrations,
    order_items::handlers::{delete_order_item, get_order_item, update_order_item},
    orders::handlers::{create_order, delete_order, get_order, list_orders, update_order},
    plants::handlers::{create_plant, delete_plant, get_plant, list_plants, update_plant},
    state::AppState,
    users::handlers::{create_user, delete_user, get_user, list_users, update_user},
};

fn main() -> Result<(), std::io::Error> {
    dotenv().ok();
    let worker_threads = num_cpus::get().max(2);
    Builder::new_multi_thread()
        .worker_threads(worker_threads)
        .enable_all()
        .build()
        .expect("Impossible de créer le runtime tokio")
        .block_on(async_main())
}

async fn async_main() -> Result<(), std::io::Error> {
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL manquant");
    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await
        .expect("Connexion base de données impossible");

    if let Err(e) = run_migrations(&pool).await {
        eprintln!("Erreur lors de l'application des migrations: {}", e);
    }

    let state = AppState::new(pool);

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
        .with(AddData::new(state.clone()))
        .with(poem::middleware::CookieJarManager::new())
        .with(cors);

    // Lancer serveur HTTP sur le port 4100
    println!("🚀 Serveur démarré sur http://0.0.0.0:4100");
    Server::new(TcpListener::bind("0.0.0.0:4100"))
        .run(app)
        .await
}
