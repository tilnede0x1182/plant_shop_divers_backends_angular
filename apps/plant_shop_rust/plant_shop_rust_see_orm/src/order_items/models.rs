//! Modeles DTO pour les items de commande.

// ==============================================================================
// Importations
// ==============================================================================

use serde::{Deserialize, Serialize};

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize, Clone)]
/// Modele d'un item de commande (lecture DB).
pub struct OrderItem {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub order_id: Option<i32>,
    pub plant_id: Option<i32>,
    pub quantity: i32,
    pub price: i32,
}

#[allow(dead_code)]
#[derive(Deserialize)]
/// DTO pour la creation/mise a jour d'un item de commande.
pub struct NewOrderItem {
    pub order_id: i32,
    pub plant_id: i32,
    pub quantity: i32,
    pub price: i32,
}
