use super::jwt::generate_jwt;
use super::models::{LoginPayload, RegisterPayload, UserAuth};
use super::session::AuthSession;
use crate::dto::UserResponse;
use crate::errors::AppError;
use crate::state::AppState;
use crate::users::models::User;
use poem::web::cookie::{Cookie, CookieJar};
/// Handlers Poem pour auth (login, register, me, logout)
use poem::{
    handler,
    web::{Data, Json},
    Result as PoemResult,
};
// use bcrypt::{verify, hash};
use argon2::password_hash::{rand_core::OsRng, PasswordHash, SaltString};
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use poem::http::StatusCode;

#[handler]
pub async fn login(
    Data(state): Data<&AppState>,
    Json(payload): Json<LoginPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserResponse>)> {
    let pool = state.read_pool();
    let user: UserAuth = sqlx::query_as::<_, UserAuth>(
        "SELECT id,email, username, password_hash, is_admin, created_at FROM users WHERE email = $1",
    )
    .bind(&payload.email)
    .fetch_optional(pool)
    .await
    .map_err(|e| AppError::DatabaseError(e))?
    .ok_or(AppError::Unauthorized)?;

    let parsed_hash = PasswordHash::new(&user.password_hash).map_err(|_| AppError::Internal)?;
    let valide = Argon2::default()
        .verify_password(payload.password.as_bytes(), &parsed_hash)
        .is_ok();

    if !valide {
        return Err(AppError::Unauthorized.into());
    }

    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let jwt = generate_jwt(user.user.id, user.user.is_admin, &jwt_secret)
        .map_err(|_| AppError::Internal)?;

    let mut cookie = Cookie::new("auth_token", jwt.clone());
    cookie.set_path("/api");
    cookie.set_http_only(false);
    cookie.set_secure(false);
    jar.add(cookie);

    Ok((
        StatusCode::CREATED,
        Json(UserResponse::from(user.user.clone())),
    ))
}

#[handler]
pub async fn register(
    Data(state): Data<&AppState>,
    Json(payload): Json<RegisterPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserResponse>)> {
    let salt = SaltString::generate(&mut OsRng);
    let hash_str = Argon2::default()
        .hash_password(payload.password.as_bytes(), &salt)
        .map_err(|_| AppError::Internal)?
        .to_string();

    let pool = state.read_pool();

    if let Some(existing) = sqlx::query_as::<_, UserAuth>(
        "SELECT id, email, username, password_hash, is_admin, created_at
		FROM users WHERE email = $1",
    )
    .bind(&payload.email)
    .fetch_optional(pool)
    .await
    .map_err(AppError::DatabaseError)?
    {
        return Ok((
            StatusCode::CREATED,
            Json(UserResponse::from(existing.user.clone())),
        ));
    }

    let user: UserAuth = sqlx::query_as::<_, UserAuth>(
        "INSERT INTO users (email, username, password_hash)
		 VALUES ($1, $2, $3)
		 RETURNING id, email, username, password_hash, is_admin, created_at",
    )
    .bind(&payload.email)
    .bind(&payload.name)
    .bind(&hash_str)
    .fetch_one(state.write_pool())
    .await
    .map_err(|e| {
        if e.as_database_error()
            .map_or(false, |db| db.is_unique_violation())
        {
            AppError::Conflict
        } else {
            AppError::DatabaseError(e)
        }
    })?;

    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let token = generate_jwt(user.user.id, user.user.is_admin, &jwt_secret)
        .map_err(|_| AppError::Internal)?;

    let mut cookie = Cookie::new("auth_token", token.clone());
    cookie.set_path("/api");
    cookie.set_http_only(false);
    cookie.set_secure(false);
    jar.add(cookie);

    Ok((
        StatusCode::CREATED,
        Json(UserResponse::from(user.user.clone())),
    ))
}

#[handler]
pub async fn me(
    Data(state): Data<&AppState>,
    auth: AuthSession,
) -> PoemResult<(StatusCode, Json<UserResponse>)> {
    let user = sqlx::query_as!(
        User,
        "SELECT id, email, username, is_admin, created_at FROM users WHERE id = $1",
        auth.user_id()
    )
    .fetch_one(state.read_pool())
    .await
    .map_err(AppError::DatabaseError)?;

    Ok((StatusCode::OK, Json(UserResponse::from(user))))
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
    jar.remove("auth_token");
    Ok(())
}
