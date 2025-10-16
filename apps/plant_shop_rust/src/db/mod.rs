/// Initialisation et accès base de données SQLx
use sqlx::{PgPool, postgres::PgPoolOptions};

pub mod migrations;
pub mod seed;

pub async fn connect_pool(database_url: &str) -> PgPool {
	PgPoolOptions::new()
		.max_connections(5)
		.connect(database_url)
		.await
		.expect("Connexion base PostgreSQL impossible")
}
