//! Extracteurs Poem pour la session et les guards admin.

// ==============================================================================
// Importations
// ==============================================================================

use std::future::Future;

use poem::web::{cookie::CookieJar, Data};
use poem::{FromRequest, Request, RequestBody};

use sea_orm::{DatabaseConnection, EntityTrait};

use crate::errors::AppError;
use crate::entity::users::Entity as Users;

use super::jwt::{verify_jwt, Claims};

// ==============================================================================
// Structures
// ==============================================================================

/// Extracteur Poem réutilisable pour centraliser la validation JWT.
pub struct AuthSession(pub Claims);

impl AuthSession {
    /// Retourne l'ID de l'utilisateur authentifie.
    ///
    /// @return ID utilisateur (i32)
    pub fn user_id(&self) -> i32 {
        self.0.sub
    }
}

/// Guard admin verifiant que l'utilisateur est admin.
pub struct AdminGuard {
    session: AuthSession,
}

// ==============================================================================
// Implementations
// ==============================================================================

impl AdminGuard {
    /// Retourne l'ID de l'utilisateur admin authentifie.
    ///
    /// @return ID utilisateur (i32)
    pub fn user_id(&self) -> i32 {
        self.session.user_id()
    }
}

impl<'a> FromRequest<'a> for AuthSession {
    /// Extrait la session d'authentification depuis la requete.
    ///
    /// @param req Reference a la requete HTTP
    /// @param body Corps de la requete
    /// @return AuthSession ou erreur si non authentifie
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
    /// Extrait et verifie que l'utilisateur est admin.
    ///
    /// @param req Reference a la requete HTTP
    /// @param body Corps de la requete
    /// @return AdminGuard ou erreur si non admin
    fn from_request(
        req: &'a Request,
        body: &mut RequestBody,
    ) -> impl Future<Output = poem::Result<Self>> + Send {
        async move {
            let auth = AuthSession::from_request(req, body).await?;
            let Data(db) = Data::<&DatabaseConnection>::from_request(req, body).await?;

            let user = Users::find_by_id(auth.user_id())
                .one(db)
                .await
                .map_err(|_| poem::Error::from(AppError::Internal))?
                .ok_or_else(|| poem::Error::from(AppError::Unauthorized))?;

            if !user.is_admin {
                return Err(poem::Error::from(AppError::Forbidden));
            }

            Ok(AdminGuard { session: auth })
        }
    }
}
