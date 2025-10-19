use serde::{Serialize, Deserialize};
use sea_orm::prelude::Decimal;
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, Clone)]
pub struct Order {
    pub id: i32,
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: Decimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PlantBasic {
    pub id: i32,
    pub name: String,
    #[serde(serialize_with = "crate::plants::models::serialize_decimal_as_i32")]
    pub price: Decimal,
    pub stock: i32,
    pub description: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct OrderItemWithPlant {
	pub id: i32,
	pub quantity: i32,
	#[serde(serialize_with = "crate::plants::models::serialize_decimal_as_i32")]
	pub price: Decimal,
	pub plant_id: i32,
	pub plant: PlantBasic,
}

#[derive(Debug, Serialize)]
pub struct OrderWithItems {
    pub id: i32,
		#[allow(dead_code)]
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: Decimal,
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
