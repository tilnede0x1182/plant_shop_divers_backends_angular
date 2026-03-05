//! DTOs de reponse API.

// ==============================================================================
// Importations
// ==============================================================================

use crate::plants::models::Plant;
use crate::users::models::User;
use chrono::{DateTime, Utc};
use serde::Serialize;

// ==============================================================================
// Structures
// ==============================================================================

/// DTO de reponse pour un utilisateur (sans mot de passe).
pub struct UserResponse {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl From<User> for UserResponse {
    /// Convertit un User en UserResponse.
    ///
    /// @param user Modele User source
    /// @return UserResponse sans mot de passe
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

/// DTO de reponse pour une plante.
pub struct PlantResponse {
    pub id: i32,
    pub name: String,
    pub description: Option<String>,
    pub price: i32,
    pub stock: i32,
    pub created_at: DateTime<Utc>,
}

impl From<Plant> for PlantResponse {
    /// Convertit un Plant en PlantResponse.
    ///
    /// @param plant Modele Plant source
    /// @return PlantResponse
    fn from(plant: Plant) -> Self {
        Self {
            id: plant.id,
            name: plant.name,
            description: plant.description,
            price: plant.price.as_i32_lossy(),
            stock: plant.stock,
            created_at: plant.created_at,
        }
    }
}

/// Plante associee a un item de commande (vue simplifiee).
pub struct OrderItemPlant {
    pub id: i32,
    pub name: String,
    pub price: i32,
}

/// DTO de reponse pour un item de commande.
pub struct OrderItemResponse {
    pub id: i32,
    #[serde(rename = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
    pub price: i32,
    pub plant: OrderItemPlant,
}

impl OrderItemResponse {
    /// Cree un nouvel OrderItemResponse.
    ///
    /// @param id ID de l'item
    /// @param plant_id ID de la plante
    /// @param quantity Quantite commandee
    /// @param price Prix unitaire
    /// @param plant Plante associee
    /// @return OrderItemResponse
    pub fn new(id: i32, plant_id: i32, quantity: i32, price: i32, plant: OrderItemPlant) -> Self {
        Self { id, plant_id, quantity, price, plant }
    }
}

/// Resume d'une commande avec ses items.
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
    /// Cree un nouvel OrderSummary.
    ///
    /// @param id ID de la commande
    /// @param status Statut
    /// @param total Prix total
    /// @param created_at Date de creation
    /// @param items Liste des items
    /// @return OrderSummary
    pub fn new(id: i32, status: String, total: i32, created_at: DateTime<Utc>, items: Vec<OrderItemResponse>) -> Self {
        Self { id, status, total, created_at, items }
    }
}
