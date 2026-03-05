//! Module de connexion a la base de donnees PostgreSQL via SeaORM.

// ==============================================================================
// Modules
// ==============================================================================

/// Migrations SQL.
pub mod migrations;
/// Seed de donnees de test.
pub mod seed;

// ==============================================================================
// Importations
// ==============================================================================

use sea_orm::DatabaseConnection;
use dotenvy::dotenv;
use sea_orm::Database;
use std::env;

// ==============================================================================
// Fonctions
// ==============================================================================

/// Connexion a la base PostgreSQL via SeaORM.
///
/// Lit DATABASE_URL depuis l'environnement et etablit la connexion.
///
/// @return DatabaseConnection prete a l'emploi
pub async fn connect_db() -> DatabaseConnection {
    dotenv().ok();
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL manquant dans .env");
    Database::connect(&database_url)
        .await
        .expect("Échec de connexion à la base PostgreSQL")
}
