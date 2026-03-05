//! Gestion de la configuration d'environnement.

// ==============================================================================
// Importations
// ==============================================================================

use std::env;

// ==============================================================================
// Structures
// ==============================================================================

/// Structure de configuration applicative.
#[allow(dead_code)]
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
        Self {
            database_url,
            jwt_secret,
            bcrypt_cost,
        }
    }
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

/// Lecture d'une valeur entiere non signee avec valeur par defaut.
///
/// @param key Nom de la variable d'environnement
/// @param default Valeur par defaut si non definie ou invalide
/// @return Valeur u64 lue ou default
pub fn env_u64(key: &str, default: u64) -> u64 {
    std::env::var(key)
        .ok()
        .and_then(|s| s.parse().ok())
        .unwrap_or(default)
}
