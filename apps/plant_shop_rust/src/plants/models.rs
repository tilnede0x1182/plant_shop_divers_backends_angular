use serde::{Serialize, Deserialize};
use uuid::Uuid;
use sqlx::types::BigDecimal;
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Plant {
    pub id: Uuid,
    pub name: String,
    pub description: Option<String>,
    pub price: BigDecimal,
    pub stock: i32,
    pub created_at: DateTime<Utc>,
}

#[derive(Deserialize)]
pub struct NewPlant {
    pub name: String,
    pub description: Option<String>,
    pub price: BigDecimal,
    pub stock: i32,
}
