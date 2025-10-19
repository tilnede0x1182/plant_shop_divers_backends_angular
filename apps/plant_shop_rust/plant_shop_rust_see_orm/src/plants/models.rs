use serde::{Serialize, Deserialize};
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, Clone)]
pub struct Plant {
	pub id: i32,
	pub name: String,
	pub price: i32,
	pub stock: i32,
	pub description: Option<String>,
	pub created_at: DateTime<Utc>,
}

#[allow(dead_code)]
#[derive(Deserialize)]
pub struct NewPlant {
	pub name: String,
	pub description: Option<String>,
	pub price: i32,
	pub stock: i32,
}

#[allow(dead_code)]
#[derive(Deserialize)]
pub struct UpdatePlant {
	pub name: Option<String>,
	pub description: Option<String>,
	pub price: Option<i32>,
	pub stock: Option<i32>,
}
