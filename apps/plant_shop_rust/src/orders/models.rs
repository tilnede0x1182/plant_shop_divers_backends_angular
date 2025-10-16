use serde::{Serialize, Deserialize};
use uuid::Uuid;
use sqlx::types::BigDecimal;
use chrono::{DateTime, Utc};
use crate::order_items::models::OrderItem;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Order {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub total: BigDecimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[derive(Serialize)]
pub struct OrderWithItems {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub total: BigDecimal,
    pub status: String,
    pub created_at: DateTime<Utc>,
    pub items: Vec<OrderItem>,
}


#[derive(Deserialize)]
pub struct OrderItemPayload {
    #[serde(rename = "plantId")]
    pub plant_id: Uuid,
    pub quantity: i32,
}

// Struct pour la mise à jour partielle du statut
#[derive(Deserialize)]
pub struct UpdateOrder {
    pub status: Option<String>,
}
