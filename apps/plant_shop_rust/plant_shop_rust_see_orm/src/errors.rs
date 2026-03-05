//! Gestion des erreurs applicatives.

// ==============================================================================
// Importations
// ==============================================================================

use poem::{error::ResponseError, http::StatusCode};
use sea_orm::DbErr;
use thiserror::Error;

// ==============================================================================
// Enums
// ==============================================================================

/// Enum des erreurs applicatives.
#[derive(Debug, Error)]
pub enum AppError {
    #[error("Non autorisé")]
    Unauthorized,
    #[error("Interdit")]
    Forbidden,
    #[error("Ressource non trouvée")]
    NotFound,
    #[error("Conflit de données")]
    Conflict,
    #[error("Erreur interne du serveur")]
    Internal,
    #[error("Erreur de base de données")]
    DatabaseError(#[from] DbErr),
}

// ==============================================================================
// Implementations
// ==============================================================================

impl ResponseError for AppError {
    /// Retourne le code HTTP correspondant a l'erreur.
    ///
    /// @return StatusCode HTTP (401, 403, 404, 409, 500)
    fn status(&self) -> StatusCode {
        match self {
            AppError::Unauthorized => StatusCode::UNAUTHORIZED,
            AppError::Forbidden => StatusCode::FORBIDDEN,
            AppError::NotFound => StatusCode::NOT_FOUND,
            AppError::Conflict => StatusCode::CONFLICT,
            AppError::Internal | AppError::DatabaseError(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}
