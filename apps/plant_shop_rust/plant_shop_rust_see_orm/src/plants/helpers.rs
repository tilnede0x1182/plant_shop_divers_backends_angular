//! Fonctions helper pour les plantes.

// ==============================================================================
// Importations
// ==============================================================================

use crate::entity::plants::ActiveModel as ActivePlant;
use crate::plants::models::UpdatePlant;
use sea_orm::Set;

// ==============================================================================
// Fonctions
// ==============================================================================

/// Applique les mises a jour sur un ActiveModel plante.
///
/// @param active Modele actif a modifier
/// @param payload Donnees de mise a jour (champs optionnels)
pub fn apply_plant_updates(active: &mut ActivePlant, payload: &UpdatePlant) {
    if let Some(name) = payload.name.clone() {
        active.name = Set(name);
    }
    if let Some(desc) = payload.description.clone() {
        active.description = Set(Some(desc));
    }
    if let Some(price) = payload.price {
        active.price = Set(price);
    }
    if let Some(stock) = payload.stock {
        active.stock = Set(stock);
    }
}
