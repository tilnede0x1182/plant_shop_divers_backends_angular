//! Modeles DTO pour les commandes.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{DateTime, Utc};
use serde::Serialize;

// ==============================================================================
// Structures
// ==============================================================================

/// DTO representant une plante associee a un item de commande.
#[derive(Serialize)]
pub struct OrderItemPlant {
    pub id: i32,
    pub name: String,
    pub price: i32,
}

/// DTO representant un item de commande en sortie API.
#[derive(Serialize)]
pub struct OrderItemResponse {
    pub id: i32,
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
    pub price: i32,
    pub plant: OrderItemPlant,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl OrderItemResponse {
    /// Cree un nouvel OrderItemResponse.
    ///
    /// @param id ID de l'item
    /// @param plant_id ID de la plante
    /// @param quantity Quantite commandee
    /// @param price Prix unitaire
    /// @param plant DTO plante associee
    /// @return OrderItemResponse
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

/// DTO representant une commande complete en sortie API.
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
    #[serde(skip_serializing_if = "Option::is_none")]
    pub number: Option<i64>,
}

impl OrderSummary {
    /// Cree un nouvel OrderSummary.
    ///
    /// @param id ID de la commande
    /// @param status Statut (pending, completed, etc.)
    /// @param total Prix total en centimes
    /// @param created_at Date de creation
    /// @param number Numero de commande (optionnel)
    /// @param items Liste des items
    /// @return OrderSummary
    pub fn new(
        id: i32,
        status: String,
        total: i32,
        created_at: DateTime<Utc>,
        number: Option<i64>,
        items: Vec<OrderItemResponse>,
    ) -> Self {
        Self {
            id,
            status,
            total,
            created_at,
            number,
            items,
        }
    }
}
