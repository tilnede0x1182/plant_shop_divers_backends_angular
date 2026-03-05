//! Migrations SQL pour SeaORM.

// ==============================================================================
// Importations
// ==============================================================================

use sea_orm::{ConnectionTrait, DatabaseConnection, DbErr};

// ==============================================================================
// Fonctions
// ==============================================================================

/// Applique la migration initiale depuis le fichier SQL brut.
///
/// Lit le script migrations/init.sql et l'exécute via la connexion SeaORM.
///
/// @param db Connexion SeaORM
/// @return Ok(()) si reussi, Err(DbErr) sinon
pub async fn run_migrations(db: &DatabaseConnection) -> Result<(), DbErr> {
    println!("📦 Application de la migration SQL initiale...");
    let sql = include_str!("../../migrations/init.sql");
    db.execute_unprepared(sql).await?;
    println!("✅ Migration SQL appliquée avec succès.");
    Ok(())
}
