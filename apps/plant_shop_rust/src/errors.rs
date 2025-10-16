/// Définition centralisée des erreurs application
use poem::{http::StatusCode};
use std::fmt;

pub enum AppError {
	Unauthorized,
	NotFound,
	Conflict,
	Internal,
}

impl fmt::Display for AppError {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		let msg = match self {
			AppError::Unauthorized => "Non autorisé",
			AppError::NotFound => "Ressource non trouvée",
			AppError::Conflict => "Conflit de données",
			AppError::Internal => "Erreur interne",
		};
		write!(f, "{msg}")
	}
}

impl std::fmt::Debug for AppError {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		fmt::Display::fmt(self, f)
	}
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
