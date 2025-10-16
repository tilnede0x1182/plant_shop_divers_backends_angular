/// Fonctions JWT pour l’authentification
use jsonwebtoken::{encode, decode, Header, Validation, EncodingKey, DecodingKey, Algorithm, errors::Error as JwtError};
use serde::{Serialize, Deserialize};
use chrono::{Utc, Duration};
use uuid::Uuid;

const EXPIRATION_HOURS: i64 = 24;

#[derive(Serialize, Deserialize)]
pub struct Claims {
	pub sub: Uuid,
	pub is_admin: bool,
	pub exp: usize,
}

pub fn generate_jwt(user_id: Uuid, is_admin: bool, secret: &str) -> Result<String, JwtError> {
	let expiration = (Utc::now() + Duration::hours(EXPIRATION_HOURS)).timestamp() as usize;
	let claims = Claims { sub: user_id, is_admin, exp: expiration };
	encode(&Header::default(), &claims, &EncodingKey::from_secret(secret.as_bytes()))
}

pub fn verify_jwt(token: &str, secret: &str) -> Result<Claims, JwtError> {
	let data = decode::<Claims>(
		token,
		&DecodingKey::from_secret(secret.as_bytes()),
		&Validation::new(Algorithm::HS256),
	)?;
	Ok(data.claims)
}
