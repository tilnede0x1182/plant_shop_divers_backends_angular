//! Modeles DTO commandes.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::types::BigDecimal;

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize, sqlx::FromRow)]
/// Modele d'une commande (lecture DB).
pub struct Order {
    pub id: i32,
    pub user_id: Option<i32>,
    #[serde(
        rename = "totalPrice",
        serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32"
    )]
    pub total: BigDecimal,
    pub status: String,
    #[serde(rename = "createdAt")]
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
/// Vue simplifiee d'une plante (id, nom, prix).
pub struct PlantBasic {
    pub id: i32,
    pub name: String,
    #[serde(serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32")]
    pub price: BigDecimal,
    pub stock: i32,
    pub description: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
/// Item de commande avec sa plante associee.
pub struct OrderItemWithPlant {
    pub id: i32,
    pub quantity: i32,
    #[serde(serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32")]
    pub price: BigDecimal,
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub plant: PlantBasic,
}

#[derive(Debug, Serialize)]
/// Commande avec tous ses items.
pub struct OrderWithItems {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub user_id: Option<i32>,
    #[serde(
        rename = "totalPrice",
        serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32"
    )]
    pub total: BigDecimal,
    pub status: String,
    #[serde(rename = "createdAt")]
    pub created_at: DateTime<Utc>,
    #[serde(rename = "orderItems")]
    pub items: Vec<OrderItemWithPlant>,
    pub number: Option<i64>,
}
