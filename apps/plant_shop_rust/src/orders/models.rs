use serde::{Serialize, Deserialize};
use sqlx::types::BigDecimal;
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Order {
    pub id: i32,
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: BigDecimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PlantBasic {
    pub id: i32,
    pub name: String,
    #[serde(serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32")]
    pub price: BigDecimal,
    pub stock: i32,
    pub description: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct OrderItemWithPlant {
	pub id: i32,
	pub quantity: i32,
	#[serde(serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32")]
	pub price: BigDecimal,
	pub plant_id: i32,
	pub plant: PlantBasic,
}

#[derive(Debug, Serialize)]
pub struct OrderWithItems {
    pub id: i32,
		#[allow(dead_code)]
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: BigDecimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
    #[serde(rename = "orderItems")]
    pub items: Vec<OrderItemWithPlant>,
}

#[derive(Deserialize)]
pub struct OrderItemPayload {
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
}

// Mise à jour partielle du statut
#[derive(Deserialize)]
pub struct UpdateOrder {
    pub status: Option<String>,
}
