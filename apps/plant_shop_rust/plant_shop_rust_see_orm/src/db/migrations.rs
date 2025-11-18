use sea_orm::{ConnectionTrait, DatabaseConnection, DbErr};

/// """ Applique la migration initiale depuis le fichier SQL brut.
/// Lit le script migrations/init.sql et l’exécute via la connexion SeaORM.
/// @db connexion SeaORM """
pub async fn run_migrations(db: &DatabaseConnection) -> Result<(), DbErr> {
    println!("📦 Application de la migration SQL initiale...");
    let sql = include_str!("../../migrations/init.sql");
    db.execute_unprepared(sql).await?;
    println!("✅ Migration SQL appliquée avec succès.");
    Ok(())
}
