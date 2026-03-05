//! Extracteurs de session.

// ==============================================================================
// Importations
// ==============================================================================

use std::future::Future;

use poem::web::{cookie::CookieJar, Data};
use poem::{FromRequest, Request, RequestBody};

use crate::errors::AppError;
use crate::state::AppState;

use super::jwt::{verify_jwt, Claims};

// ==============================================================================
// Structures
// ==============================================================================

/// Session d'authentification contenant les claims JWT.
pub struct AuthSession(pub Claims);

/// Guard verifiant que l'utilisateur est admin.
pub struct AdminGuard {
    session: AuthSession,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl AuthSession {
    /// Retourne l'ID de l'utilisateur authentifie.
    ///
    /// @return ID utilisateur (i32)
    pub fn user_id(&self) -> i32 {
        self.0.sub
    }
}

impl AdminGuard {
    /// Retourne l'ID de l'utilisateur admin.
    ///
    /// @return ID utilisateur (i32)
    pub fn user_id(&self) -> i32 {
        self.session.user_id()
    }
}

impl<'a> FromRequest<'a> for AuthSession {
    fn from_request(
        req: &'a Request,
        body: &mut RequestBody,
    ) -> impl Future<Output = poem::Result<Self>> + Send {
        let fut = <&CookieJar>::from_request(req, body);
        async move {
            let jar = fut.await?;

            let token = jar
                .get("auth_token")
                .map(|c| c.value_str().to_string())
                .ok_or_else(|| poem::Error::from(AppError::Unauthorized))?;

            let secret = std::env::var("JWT_SECRET")
                .map_err(|_| AppError::Internal)
                .map_err(poem::Error::from)?;
            let claims = verify_jwt(&token, &secret)
                .map_err(|_| AppError::Unauthorized)
                .map_err(poem::Error::from)?;

            Ok(Self(claims))
        }
    }
}

impl<'a> FromRequest<'a> for AdminGuard {
    fn from_request(
        req: &'a Request,
        body: &mut RequestBody,
    ) -> impl Future<Output = poem::Result<Self>> + Send {
        async move {
            let auth = AuthSession::from_request(req, body).await?;
            let Data(state) = Data::<&AppState>::from_request(req, body).await?;
            let pool = state.read_pool();

            let record = sqlx::query!("SELECT is_admin FROM users WHERE id = $1", auth.user_id())
                .fetch_one(pool)
                .await
                .map_err(|e| poem::Error::from(AppError::DatabaseError(e)))?;

            if !record.is_admin {
                return Err(poem::Error::from(AppError::Forbidden));
            }

            Ok(AdminGuard { session: auth })
        }
    }
}
