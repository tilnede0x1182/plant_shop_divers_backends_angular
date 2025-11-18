use super::jwt::generate_jwt;
use super::models::{LoginPayload, RegisterPayload, UserAuth};
use super::session::AuthSession;
use crate::errors::AppError;
use crate::users::models::User;
use poem::web::cookie::{Cookie, CookieJar};
/// Handlers Poem pour auth (login, register, me, logout)
use poem::{
    handler,
    web::{Data, Json},
    Result as PoemResult,
};
use sqlx::PgPool;
// use bcrypt::{verify, hash};
use argon2::password_hash::{rand_core::OsRng, PasswordHash, SaltString};
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use poem::http::StatusCode;

#[handler]
pub async fn login(
    Data(pool): Data<&PgPool>,
    Json(payload): Json<LoginPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserAuth>)> {
    let LoginPayload { email, password } = payload;

    let user: UserAuth = sqlx::query_as::<_, UserAuth>(
        "SELECT id,email, username, password_hash, is_admin, created_at FROM users WHERE email = $1",
    )
    .bind(&email)
    .fetch_optional(pool)
    .await
    .map_err(|e| AppError::DatabaseError(e))?
    .ok_or(AppError::Unauthorized)?;

    let parsed_hash = PasswordHash::new(&user.password_hash).map_err(|_| AppError::Internal)?;
    let valide = Argon2::default()
        .verify_password(password.as_bytes(), &parsed_hash)
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

    Ok((StatusCode::CREATED, Json(user)))
}

#[handler]
pub async fn register(
    Data(pool): Data<&PgPool>,
    Json(payload): Json<RegisterPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserAuth>)> {
    let RegisterPayload {
        name,
        email,
        password,
    } = payload;
    let salt = SaltString::generate(&mut OsRng);
    let hash_str = Argon2::default()
        .hash_password(password.as_bytes(), &salt)
        .map_err(|_| AppError::Internal)?
        .to_string();

    // ✅ Vérifie si l'utilisateur existe déjà
    if let Some(existing) = sqlx::query_as::<_, UserAuth>(
        "SELECT id, email, username, password_hash, is_admin, created_at
		FROM users WHERE email = $1",
    )
    .bind(&email)
    .fetch_optional(pool)
    .await
    .map_err(AppError::DatabaseError)?
    {
        return Ok((StatusCode::CREATED, Json(existing)));
    }

    // 🧩 Création d’un nouvel utilisateur
    let user: UserAuth = sqlx::query_as::<_, UserAuth>(
        "INSERT INTO users (email, username, password_hash)
		 VALUES ($1, $2, $3)
		 RETURNING id, email, username, password_hash, is_admin, created_at",
    )
    .bind(&email)
    .bind(&name)
    .bind(&hash_str)
    .fetch_one(pool)
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

    // 🔐 Génère le cookie JWT
    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let token = generate_jwt(user.user.id, user.user.is_admin, &jwt_secret)
        .map_err(|_| AppError::Internal)?;

    let mut cookie = Cookie::new("auth_token", token.clone());
    cookie.set_path("/api");
    cookie.set_http_only(false);
    cookie.set_secure(false);
    jar.add(cookie);

    Ok((StatusCode::CREATED, Json(user)))
}

#[handler]
pub async fn me(
    Data(pool): Data<&PgPool>,
    auth: AuthSession,
) -> PoemResult<(StatusCode, Json<User>)> {
    let user = sqlx::query_as!(
        User,
        "SELECT id, email, username, is_admin, created_at FROM users WHERE id = $1",
        auth.user_id()
    )
    .fetch_one(pool)
    .await
    .map_err(AppError::DatabaseError)?;

    Ok((StatusCode::OK, Json(user)))
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
    jar.remove("auth_token");
    Ok(())
}
