//! Modeles DTO pour les utilisateurs.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

use crate::entity::users::Model as UserModel;

// ==============================================================================
// Structures
// ==============================================================================

/// DTO representant un utilisateur en sortie API.
#[derive(Serialize, Deserialize, Clone)]
pub struct User {
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

impl From<UserModel> for User {
    /// Convertit un UserModel en DTO User.
    ///
    /// @param model Modele SeaORM source
    /// @return DTO User
    fn from(model: UserModel) -> Self {
        Self {
            id: model.id,
            email: model.email,
            username: model.username,
            is_admin: model.is_admin,
            created_at: model.created_at.into(),
        }
    }
}

impl From<&UserModel> for User {
    /// Convertit une reference UserModel en DTO User.
    ///
    /// @param model Reference au modele SeaORM
    /// @return DTO User
    fn from(model: &UserModel) -> Self {
        Self {
            id: model.id,
            email: model.email.clone(),
            username: model.username.clone(),
            is_admin: model.is_admin,
            created_at: model.created_at.into(),
        }
    }
}

/// DTO pour la mise a jour d'un utilisateur (champs optionnels).
#[derive(Deserialize, Serialize, Debug)]
pub struct UpdateUser {
    #[serde(alias = "name")]
    pub name: Option<String>,
    pub email: Option<String>,
    #[serde(alias = "admin")]
    pub admin: Option<bool>,
}

/// DTO pour la creation d'un utilisateur.
#[allow(dead_code)]
#[derive(Deserialize)]
pub struct NewUser {
    pub name: String,
    pub email: String,
    pub password: String,
}
