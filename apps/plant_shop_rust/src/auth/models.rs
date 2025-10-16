/// Structures liées à l'authentification (DTO, User minimal)
use serde::{Serialize, Deserialize};
use uuid::Uuid;

#[derive(Serialize, Deserialize)]
pub struct AuthPayload {
	pub email: String,
	pub password: String,
}

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct UserAuth {
	pub id: Uuid,
	pub email: String,
	pub username: String,
	pub password_hash: String,
	pub is_admin: bool,
	pub created_at: chrono::DateTime<chrono::Utc>,
}
