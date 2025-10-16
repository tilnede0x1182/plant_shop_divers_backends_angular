use serde::{Serialize, Deserialize};
use uuid::Uuid;
use sqlx::types::BigDecimal;

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct OrderItem {
    pub id: Uuid,
    pub order_id: Option<Uuid>,
    pub plant_id: Option<Uuid>,
    pub quantity: i32,
    pub price: BigDecimal,
}

#[derive(Deserialize)]
pub struct NewOrderItem {
    pub order_id: Uuid,
    pub plant_id: Uuid,
    pub quantity: i32,
    pub price: BigDecimal,
}
