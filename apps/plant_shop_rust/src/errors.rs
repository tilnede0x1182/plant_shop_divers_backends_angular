/// Définition centralisée des erreurs application
use poem::{Error, http::StatusCode};
use thiserror::Error;

#[derive(Error, Debug)]
pub enum AppError {
	#[error("Non autorisé")]
	Unauthorized,
	#[error("Ressource non trouvée")]
	NotFound,
	#[error("Conflit de données")]
	Conflict,
	#[error("Erreur interne")]
	Internal,
}

impl poem::IntoResponse for AppError {
	fn into_response(self) -> poem::Response {
		let status = match self {
			AppError::Unauthorized => StatusCode::UNAUTHORIZED,
			AppError::NotFound => StatusCode::NOT_FOUND,
			AppError::Conflict => StatusCode::CONFLICT,
			AppError::Internal => StatusCode::INTERNAL_SERVER_ERROR,
		};
		poem::Response::builder().status(status).body(self.to_string())
	}
}

impl From<anyhow::Error> for AppError {
	fn from(_: anyhow::Error) -> Self {
		AppError::Internal
	}
}
