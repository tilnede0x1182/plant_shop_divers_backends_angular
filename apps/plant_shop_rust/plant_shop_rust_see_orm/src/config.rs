/// Gestion de la configuration d'environnement
use std::env;

#[allow(dead_code)]
pub struct Config {
    pub database_url: String,
    pub jwt_secret: String,
    pub bcrypt_cost: u32,
}

impl Config {
    /// Charge la configuration à partir des variables d'environnement
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

/// Lecture d'une valeur entière non signée avec valeur par défaut.
pub fn env_u64(key: &str, default: u64) -> u64 {
    std::env::var(key)
        .ok()
        .and_then(|s| s.parse().ok())
        .unwrap_or(default)
}
