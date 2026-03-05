//! Modeles pour l'authentification.

// ==============================================================================
// Importations
// ==============================================================================

use serde::{Deserialize, Serialize};

// ==============================================================================
// Structures
// ==============================================================================

/// Payload pour l'inscription d'un utilisateur.
#[derive(Serialize, Deserialize)]
pub struct RegisterPayload {
    pub name: String,
    pub email: String,
    pub password: String,
}

/// Payload pour la connexion d'un utilisateur.
#[derive(Serialize, Deserialize)]
pub struct LoginPayload {
    pub email: String,
    pub password: String,
}

/// Utilisateur avec hash du mot de passe (pour verification auth).
#[derive(Serialize, Deserialize, Clone)]
pub struct UserAuth {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub email: String,
    pub username: String,
    pub password_hash: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
