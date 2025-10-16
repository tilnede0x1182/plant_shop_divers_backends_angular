/// Structures Plant (liste, création)
use serde::{Serialize, Deserialize};
use uuid::Uuid;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Plant {
	pub id: Uuid,
	pub name: String,
	pub description: Option<String>,
	pub price: f64,
	pub stock: i32,
	pub created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Deserialize)]
pub struct NewPlant {
	pub name: String,
	pub description: Option<String>,
	pub price: f64,
	pub stock: i32,
}
