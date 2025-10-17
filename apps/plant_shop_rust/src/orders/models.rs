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

#[derive(Debug, Serialize)]
pub struct PlantBasic {
    pub id: i32,
    #[serde(skip_serializing)]
    pub name: String,
    pub price: BigDecimal,
    pub stock: i32,
    pub description: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct OrderItemWithPlant {
    pub id: i32,
    #[serde(skip_serializing)]
    pub quantity: i32,
    pub price: BigDecimal,
    pub plant: PlantBasic,
}

#[derive(Debug, Serialize)]
pub struct OrderWithItems {
    pub id: i32,
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    pub total: BigDecimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
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
