/// Structures OrderItem (public, création)
use serde::{Serialize, Deserialize};
use uuid::Uuid;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct OrderItem {
	pub id: Uuid,
	pub order_id: Uuid,
	pub plant_id: Uuid,
	pub quantity: i32,
	pub price: f64,
}

#[derive(Deserialize)]
pub struct NewOrderItem {
	pub order_id: Uuid,
	pub plant_id: Uuid,
	pub quantity: i32,
	pub price: f64,
}
