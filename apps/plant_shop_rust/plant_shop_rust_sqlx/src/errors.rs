/// Définition centralisée des erreurs application
use poem::{error::ResponseError, http::StatusCode};
use thiserror::Error;

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
    DatabaseError(#[from] sqlx::Error),
}

impl ResponseError for AppError {
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
