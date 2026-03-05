//! Configuration de l'application.

// ==============================================================================
// Importations
// ==============================================================================

use std::env;

// ==============================================================================
// Structures
// ==============================================================================

#[allow(dead_code)]
/// Configuration applicative (DB, JWT, bcrypt).
pub struct Config {
	pub database_url: String,
	pub jwt_secret: String,
	pub bcrypt_cost: u32,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl Config {
	/// Charge la configuration a partir des variables d'environnement.
	///
	/// @return Config avec DATABASE_URL, JWT_SECRET, BCRYPT_COST
	#[allow(dead_code)]
	pub fn from_env() -> Self {
		let database_url = env::var("DATABASE_URL").expect("DATABASE_URL manquant");
		let jwt_secret = env::var("JWT_SECRET").expect("JWT_SECRET manquant");
		let bcrypt_cost = env::var("BCRYPT_COST")
			.ok()
			.and_then(|s| s.parse().ok())
			.unwrap_or(12);
		Self { database_url, jwt_secret, bcrypt_cost }
	}
}
