//! Module d'authentification.

// ==============================================================================
// Modules
// ==============================================================================

/// Handlers HTTP pour l'authentification.
pub mod handlers;
/// Fonctions de generation et verification JWT.
pub mod jwt;
/// Modeles d'authentification (payloads, UserAuth).
pub mod models;
/// Extracteurs de session (AuthSession, AdminGuard).
pub mod session;
