//! Modeles DTO pour les plantes.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

use crate::entity::plants::Model as PlantModel;

// ==============================================================================
// Structures
// ==============================================================================

/// DTO representant une plante en sortie API.
#[derive(Serialize, Deserialize, Clone)]
pub struct Plant {
    pub id: i32,
    pub name: String,
    pub price: i32,
    pub stock: i32,
    pub description: Option<String>,
    pub created_at: DateTime<Utc>,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl From<PlantModel> for Plant {
    /// Convertit un PlantModel en DTO Plant.
    ///
    /// @param model Modele SeaORM source
    /// @return DTO Plant
    fn from(model: PlantModel) -> Self {
        Self {
            id: model.id,
            name: model.name,
            price: model.price,
            stock: model.stock,
            description: model.description,
            created_at: model.created_at.into(),
        }
    }
}

impl From<&PlantModel> for Plant {
    /// Convertit une reference PlantModel en DTO Plant.
    ///
    /// @param model Reference au modele SeaORM
    /// @return DTO Plant
    fn from(model: &PlantModel) -> Self {
        Self {
            id: model.id,
            name: model.name.clone(),
            price: model.price,
            stock: model.stock,
            description: model.description.clone(),
            created_at: model.created_at.into(),
        }
    }
}

/// DTO pour la creation d'une plante.
#[allow(dead_code)]
#[derive(Deserialize)]
pub struct NewPlant {
    pub name: String,
    pub description: Option<String>,
    pub price: i32,
    pub stock: i32,
}

/// DTO pour la mise a jour d'une plante (champs optionnels).
#[allow(dead_code)]
#[derive(Deserialize)]
pub struct UpdatePlant {
    pub name: Option<String>,
    pub description: Option<String>,
    pub price: Option<i32>,
    pub stock: Option<i32>,
}
