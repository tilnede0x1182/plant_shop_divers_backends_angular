/// Structures Order (public, création)
use serde::{Serialize, Deserialize};
use uuid::Uuid;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Order {
	pub id: Uuid,
	pub user_id: Uuid,
	pub total: f64,
	pub created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Deserialize)]
pub struct NewOrder {
	pub user_id: Uuid,
	pub total: f64,
}
