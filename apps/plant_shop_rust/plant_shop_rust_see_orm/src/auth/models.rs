/// Structures liées à l'authentification (DTO, User minimal)
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct RegisterPayload {
    pub name: String,
    pub email: String,
    pub password: String,
}

#[derive(Serialize, Deserialize)]
pub struct LoginPayload {
    pub email: String,
    pub password: String,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct UserAuth {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub email: String,
    pub username: String,
    pub password_hash: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
