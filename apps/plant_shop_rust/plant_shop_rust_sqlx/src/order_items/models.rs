//! Modeles DTO items de commande.

// ==============================================================================
// Importations
// ==============================================================================

use serde::{Deserialize, Serialize};
use sqlx::types::BigDecimal;

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize, sqlx::FromRow)]
/// Modele d'un item de commande (lecture DB).
pub struct OrderItem {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub order_id: Option<i32>,
    pub plant_id: Option<i32>,
    pub quantity: i32,
    #[serde(serialize_with = "crate::plants::models::serialize_bigdecimal_as_i32")]
    pub price: BigDecimal,
}

#[derive(Deserialize)]
/// DTO pour la creation/mise a jour d'un item de commande.
pub struct NewOrderItem {
    pub order_id: i32,
    pub plant_id: i32,
    pub quantity: i32,
    pub price: BigDecimal,
}
