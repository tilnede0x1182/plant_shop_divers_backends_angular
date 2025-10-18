/// Structures liées à l'authentification (DTO, User minimal)
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize)]
pub struct AuthPayload {
	pub email: String,
	pub username: String,
	pub password: String,
}

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct UserAuth {
    pub id: i32,
    #[serde(skip_serializing)]
    pub email: String,
    pub username: String,
    pub password_hash: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
