//! Configuration du logging.

// ==============================================================================
// Importations
// ==============================================================================

use std::sync::OnceLock;

use crate::errors::AppError;

// ==============================================================================
// Constantes
// ==============================================================================

static DEBUG_ENABLED: OnceLock<bool> = OnceLock::new();

// ==============================================================================
// Fonctions
// ==============================================================================

/// Verifie si le mode debug est active (variable DEBUG).
///
/// @return true si DEBUG=1/true/on/yes
fn is_enabled() -> bool {
    *DEBUG_ENABLED.get_or_init(|| match std::env::var("DEBUG") {
        Ok(value) => matches!(value.to_lowercase().as_str(), "1" | "true" | "on" | "yes"),
        Err(_) => false,
    })
}

/// Log un message en mode debug (si active).
///
/// @param message Message a logger
#[allow(dead_code)]
pub fn log_debug(message: impl AsRef<str>) {
    if is_enabled() {
        eprintln!("[DEBUG] {}", message.as_ref());
    }
}

/// Log un message en mode debug (lazy evaluation).
///
/// @param builder Closure construisant le message
pub fn log_debug_lazy<F>(builder: F)
where
    F: FnOnce() -> String,
{
    if is_enabled() {
        eprintln!("[DEBUG] {}", builder());
    }
}

/// Log une erreur en mode debug.
///
/// @param message Message descriptif
/// @param error Erreur applicative
#[allow(dead_code)]
pub fn log_error(message: impl AsRef<str>, error: &AppError) {
    if is_enabled() {
        eprintln!("[DEBUG][ERROR] {} -> {}", message.as_ref(), error);
    }
}
