use chrono::{DateTime, Utc};
use serde::Serialize;

use crate::plants::models::{Plant, PriceExt};
use crate::users::models::User;

#[derive(Serialize, Clone)]
pub struct UserResponse {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    #[serde(rename = "createdAt")]
    pub created_at: DateTime<Utc>,
}

impl From<User> for UserResponse {
    fn from(user: User) -> Self {
        Self {
            id: user.id,
            email: user.email,
            username: user.username,
            is_admin: user.is_admin,
            created_at: user.created_at,
        }
    }
}

#[derive(Serialize, Clone)]
pub struct PlantResponse {
    pub id: i32,
    pub name: String,
    pub price: i32,
    pub stock: i32,
    pub description: Option<String>,
    #[serde(rename = "createdAt")]
    pub created_at: DateTime<Utc>,
}

impl From<Plant> for PlantResponse {
    fn from(model: Plant) -> Self {
        Self {
            id: model.id,
            name: model.name,
            price: model.price.as_i32_lossy(),
            stock: model.stock,
            description: model.description,
            created_at: model.created_at,
        }
    }
}

#[derive(Serialize, Clone)]
pub struct OrderItemPlant {
    pub id: i32,
    pub name: String,
    pub price: i32,
}

#[derive(Serialize, Clone)]
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

#[derive(Serialize, Clone)]
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
