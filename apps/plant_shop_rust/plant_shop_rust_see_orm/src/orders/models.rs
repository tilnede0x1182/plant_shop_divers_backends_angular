use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Serialize)]
pub struct OrderItemPlant {
    pub id: i32,
    pub name: String,
    pub price: i32,
}

#[derive(Serialize)]
pub struct OrderItemResponse {
    pub id: i32,
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
    pub price: i32,
    pub plant: OrderItemPlant,
}

impl OrderItemResponse {
    pub fn new(id: i32, plant_id: i32, quantity: i32, price: i32, plant: OrderItemPlant) -> Self {
        Self {
            id,
            plant_id,
            quantity,
            price,
            plant,
        }
    }
}

#[derive(Serialize)]
pub struct OrderSummary {
    pub id: i32,
    pub status: String,
    #[serde(rename = "totalPrice")]
    pub total: i32,
    #[serde(rename = "createdAt")]
    pub created_at: DateTime<Utc>,
    #[serde(rename = "orderItems")]
    pub items: Vec<OrderItemResponse>,
}

impl OrderSummary {
    pub fn new(
        id: i32,
        status: String,
        total: i32,
        created_at: DateTime<Utc>,
        items: Vec<OrderItemResponse>,
    ) -> Self {
        Self {
            id,
            status,
            total,
            created_at,
            items,
        }
    }
}
