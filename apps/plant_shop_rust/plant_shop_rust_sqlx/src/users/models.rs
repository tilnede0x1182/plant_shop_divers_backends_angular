//! Modeles DTO utilisateurs.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize, sqlx::FromRow, Clone)]
/// Modele d'un utilisateur (lecture DB).
pub struct User {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}

#[derive(Deserialize, Serialize, Debug)]
/// DTO pour la mise a jour d'un utilisateur.
pub struct UpdateUser {
    pub name: Option<String>,
    pub email: Option<String>,
    pub admin: Option<bool>,
}

#[derive(Deserialize)]
/// DTO pour la creation d'un utilisateur.
pub struct NewUser {
    pub name: String,
    pub email: String,
    pub password: String,
}
