use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[allow(dead_code)]
#[derive(Serialize, Deserialize, Clone)]
pub struct Order {
    pub id: i32,
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: i32,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PlantBasic {
    pub id: i32,
    pub name: String,
    pub price: i32,
    pub stock: i32,
    pub description: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct OrderItemWithPlant {
    pub id: i32,
    pub quantity: i32,
    pub price: i32,
    pub plant_id: i32,
    pub plant: PlantBasic,
}

#[derive(Debug, Serialize)]
pub struct OrderWithItems {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: i32,
    pub status: String,
    pub created_at: DateTime<Utc>,
    #[serde(rename = "orderItems")]
    pub items: Vec<OrderItemWithPlant>,
}

#[allow(dead_code)]
#[derive(Deserialize)]
pub struct OrderItemPayload {
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
}

#[allow(dead_code)]
#[derive(Deserialize)]
pub struct UpdateOrder {
    pub status: Option<String>,
}
