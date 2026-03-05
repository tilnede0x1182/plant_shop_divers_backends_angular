//! Fonctions helper pour les utilisateurs.

// ==============================================================================
// Importations
// ==============================================================================

use crate::entity::users::ActiveModel as ActiveUser;
use crate::users::models::UpdateUser;
use sea_orm::Set;

// ==============================================================================
// Fonctions
// ==============================================================================

/// Applique les mises a jour sur un ActiveModel utilisateur.
///
/// @param active Modele actif a modifier
/// @param payload Donnees de mise a jour (champs optionnels)
/// @param can_toggle_admin True si l'appelant peut modifier is_admin
pub fn apply_user_updates(active: &mut ActiveUser, payload: &UpdateUser, can_toggle_admin: bool) {
    if let Some(name) = payload.name.clone() {
        active.username = Set(name);
    }
    if let Some(email) = payload.email.clone() {
        active.email = Set(email);
    }
    if can_toggle_admin {
        if let Some(admin) = payload.admin {
            active.is_admin = Set(admin);
        }
    }
}
