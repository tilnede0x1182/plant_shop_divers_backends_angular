//! Fonctions JWT pour l'authentification.

// ==============================================================================
// Importations
// ==============================================================================

use chrono::{Duration, Utc};
use jsonwebtoken::{
    decode, encode, errors::Error as JwtError, Algorithm, DecodingKey, EncodingKey, Header,
    Validation,
};
use serde::{Deserialize, Serialize};

// ==============================================================================
// Constantes
// ==============================================================================

const EXPIRATION_HOURS: i64 = 24;

// ==============================================================================
// Structures
// ==============================================================================

/// Claims du token JWT.
#[derive(Serialize, Deserialize)]
pub struct Claims {
    pub sub: i32,
    pub is_admin: bool,
    pub exp: usize,
}

// ==============================================================================
// Fonctions
// ==============================================================================

/// Genere un token JWT pour un utilisateur.
///
/// # Arguments
/// * `user_id` - ID de l'utilisateur
/// * `is_admin` - True si admin
/// * `secret` - Cle secrete JWT
///
/// # Returns
/// Token JWT ou erreur
pub fn generate_jwt(user_id: i32, is_admin: bool, secret: &str) -> Result<String, JwtError> {
    let expiration = (Utc::now() + Duration::hours(EXPIRATION_HOURS)).timestamp() as usize;
    let claims = Claims {
        sub: user_id,
        is_admin,
        exp: expiration,
    };
    encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )
}

/// Verifie et decode un token JWT.
///
/// # Arguments
/// * `token` - Token JWT a verifier
/// * `secret` - Cle secrete JWT
///
/// # Returns
/// Claims decodes ou erreur
pub fn verify_jwt(token: &str, secret: &str) -> Result<Claims, JwtError> {
    // Nettoie d’éventuelles guillemets ajoutés par certains clients HTTP
    let cleaned = token.trim_matches('"');
    let data = decode::<Claims>(
        cleaned,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::new(Algorithm::HS256),
    )?;
    Ok(data.claims)
}
