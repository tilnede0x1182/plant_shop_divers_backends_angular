pub mod migrations;
pub mod seed;

use sea_orm::DatabaseConnection;

/// Module `db` simplifié pour SeaORM
/// Ne crée plus de `PgPool` (SQLx), la connexion est fournie via `connect_db()`.
use sea_orm::Database;
use dotenvy::dotenv;
use std::env;

/// Connexion à la base PostgreSQL via SeaORM.
pub async fn connect_db() -> DatabaseConnection {
	dotenv().ok();
	let database_url = env::var("DATABASE_URL").expect("DATABASE_URL manquant dans .env");
	Database::connect(&database_url)
		.await
		.expect("Échec de connexion à la base PostgreSQL")
}
