//! Migrations SQLx.

// ==============================================================================
// Importations
// ==============================================================================

use sqlx::{migrate::MigrateError, migrate::Migrator, Pool, Postgres};

// ==============================================================================
// Constantes
// ==============================================================================

// Le chemin doit être relatif à la racine du crate (où se trouve Cargo.toml)
static MIGRATOR: Migrator = sqlx::migrate!("./migrations");

// ==============================================================================
// Fonctions
// ==============================================================================

/// Execute les migrations sur la base de donnees.
///
/// @param pool Pool PostgreSQL
/// @return Ok(()) ou erreur de migration
pub async fn run_migrations(pool: &Pool<Postgres>) -> Result<(), MigrateError> {
    MIGRATOR.run(pool).await
}
