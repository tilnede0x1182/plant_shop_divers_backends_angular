/// Structures User (public, private, update)
use serde::{Serialize, Deserialize};
use uuid::Uuid;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct User {
	pub id: Uuid,
	pub email: String,
	pub username: String,
	pub is_admin: bool,
	pub created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Deserialize)]
pub struct UpdateUser {
	pub username: Option<String>,
	pub email: Option<String>,
}
