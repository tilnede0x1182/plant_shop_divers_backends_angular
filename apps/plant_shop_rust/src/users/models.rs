use serde::{Serialize, Deserialize};
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct User {
	pub id: i32,
	pub email: String,
	#[serde(rename = "name")]
	pub username: String,
	#[serde(rename = "admin")]
	pub is_admin: bool,
	pub created_at: DateTime<Utc>,
}

#[derive(Deserialize)]
pub struct UpdateUser {
	pub name: Option<String>,
	pub email: Option<String>,
}

#[derive(Deserialize)]
pub struct NewUser {
    pub name: String,
    pub email: String,
    pub password: String,
}
