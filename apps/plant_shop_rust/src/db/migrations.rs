/// Gestion des migrations SQLx
use sqlx::{Pool, Postgres, migrate::Migrator};

static MIGRATOR: Migrator = sqlx::migrate!("migrations");

pub async fn run_migrations(pool: &Pool<Postgres>) -> Result<(), sqlx::Error> {
	MIGRATOR.run(pool).await
}
