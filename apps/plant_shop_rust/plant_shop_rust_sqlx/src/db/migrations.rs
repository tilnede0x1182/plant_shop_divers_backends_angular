/// Gestion des migrations SQLx
use sqlx::{migrate::MigrateError, migrate::Migrator, Pool, Postgres};

// Le chemin doit être relatif à la racine du crate (où se trouve Cargo.toml)
static MIGRATOR: Migrator = sqlx::migrate!("./migrations");

pub async fn run_migrations(pool: &Pool<Postgres>) -> Result<(), MigrateError> {
    MIGRATOR.run(pool).await
}
