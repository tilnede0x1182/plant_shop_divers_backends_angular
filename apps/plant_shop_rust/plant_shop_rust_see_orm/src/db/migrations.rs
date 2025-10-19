use sea_orm_migration::prelude::*;
use sea_orm::DatabaseConnection;

/// Applique la migration initiale depuis le fichier SQL brut.
pub async fn run_migrations(db: &DatabaseConnection) -> Result<(), DbErr> {
	println!("📦 Application de la migration SQL initiale...");
	let sql = include_str!("../../migrations/init.sql");
	db.execute_unprepared(sql).await?;
	println!("✅ Migration SQL appliquée avec succès.");
	Ok(())
}
