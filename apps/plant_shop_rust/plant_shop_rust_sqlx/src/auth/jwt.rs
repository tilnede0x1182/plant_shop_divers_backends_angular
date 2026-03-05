//! Fonctions JWT.

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

#[derive(Serialize, Deserialize)]
/// Claims du token JWT (sub, is_admin, exp).
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
/// @param user_id ID de l'utilisateur
/// @param is_admin Statut admin
/// @param secret Cle secrete JWT
/// @return Token JWT ou erreur
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
/// @param token Token JWT a verifier
/// @param secret Cle secrete JWT
/// @return Claims ou erreur
pub fn verify_jwt(token: &str, secret: &str) -> Result<Claims, JwtError> {
    let cleaned = token.trim_matches('"');
    let data = decode::<Claims>(
        cleaned,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::new(Algorithm::HS256),
    )?;
    Ok(data.claims)
}
