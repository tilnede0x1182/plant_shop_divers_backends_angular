use chrono::{Duration, Utc};
/// Fonctions JWT pour l’authentification
use jsonwebtoken::{
    decode, encode, errors::Error as JwtError, Algorithm, DecodingKey, EncodingKey, Header,
    Validation,
};
use serde::{Deserialize, Serialize};

const EXPIRATION_HOURS: i64 = 24;

#[derive(Serialize, Deserialize)]
pub struct Claims {
    pub sub: i32,
    pub is_admin: bool,
    pub exp: usize,
}

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
