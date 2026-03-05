//! Modeles d'authentification.

// ==============================================================================
// Importations
// ==============================================================================

use serde::{Deserialize, Serialize};
use crate::users::models::User;
use sqlx::{postgres::PgRow, FromRow, Row};

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize)]
/// Payload pour l'inscription utilisateur.
pub struct RegisterPayload {
    pub name: String,
    pub email: String,
    pub password: String,
}

#[derive(Serialize, Deserialize)]
/// Payload pour la connexion utilisateur.
pub struct LoginPayload {
    pub email: String,
    pub password: String,
}

#[derive(Serialize, Deserialize, Clone)]
/// Utilisateur avec hash du mot de passe (verification auth).
pub struct UserAuth {
    #[serde(flatten)]
    pub user: User,
    #[serde(skip_serializing)]
    pub password_hash: String,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl From<UserAuth> for User {
    /// Convertit un UserAuth en User (sans mot de passe).
    ///
    /// @param ua UserAuth source
    /// @return User
    fn from(value: UserAuth) -> Self {
        value.user
    }
}

impl<'r> FromRow<'r, PgRow> for UserAuth {
    fn from_row(row: &'r PgRow) -> Result<Self, sqlx::Error> {
        Ok(Self {
            user: User {
                id: row.try_get("id")?,
                email: row.try_get("email")?,
                username: row.try_get("username")?,
                is_admin: row.try_get("is_admin")?,
                created_at: row.try_get("created_at")?,
            },
            password_hash: row.try_get("password_hash")?,
        })
    }
}
