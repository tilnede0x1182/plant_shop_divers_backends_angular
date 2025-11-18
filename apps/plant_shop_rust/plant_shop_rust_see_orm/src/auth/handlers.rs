use crate::auth::jwt::generate_jwt;
use crate::auth::session::AuthSession;
use crate::entity::users::{ActiveModel as ActiveUser, Column, Entity as User};
use crate::errors::AppError;
use crate::users::models::User as UserDto;
use argon2::password_hash::{rand_core::OsRng, PasswordHash, SaltString};
use argon2::{Argon2, PasswordHasher, PasswordVerifier};
use once_cell::sync::Lazy;
/// Handlers Poem pour auth (login, register, me, logout) — version SeaORM
use poem::{
    handler,
    http::StatusCode,
    web::cookie::{Cookie, CookieJar},
    web::{Data, Json},
    Result as PoemResult,
};
use sea_orm::{ActiveModelTrait, ColumnTrait, DatabaseConnection, EntityTrait, QueryFilter, Set};

static ARGON2: Lazy<Argon2> = Lazy::new(Argon2::default);

/// Payload de login
#[derive(serde::Deserialize)]
pub struct LoginPayload {
    pub email: String,
    pub password: String,
}

/// Payload de register
#[derive(serde::Deserialize)]
pub struct RegisterPayload {
    pub email: String,
    #[serde(alias = "name")]
    pub username: String,
    pub password: String,
}

#[handler]
pub async fn login(
    Data(db): Data<&DatabaseConnection>,
    Json(payload): Json<LoginPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserDto>)> {
    let user = User::find()
        .filter(Column::Email.eq(payload.email.clone()))
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::Unauthorized)?;

    let parsed = PasswordHash::new(&user.password_hash).map_err(|_| AppError::Internal)?;
    let ok = ARGON2
        .verify_password(payload.password.as_bytes(), &parsed)
        .is_ok();
    if !ok {
        return Err(AppError::Unauthorized.into());
    }

    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let jwt = generate_jwt(user.id, user.is_admin, &jwt_secret).map_err(|_| AppError::Internal)?;

    let mut cookie = Cookie::new("auth_token", jwt.clone());
    cookie.set_path("/api");
    cookie.set_http_only(false);
    cookie.set_secure(false);
    jar.add(cookie);

    Ok((StatusCode::CREATED, Json(UserDto::from(user))))
}

#[handler]
pub async fn register(
    Data(db): Data<&DatabaseConnection>,
    Json(payload): Json<RegisterPayload>,
    jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserDto>)> {
    let salt = SaltString::generate(&mut OsRng);
    let hash_str = ARGON2
        .hash_password(payload.password.as_bytes(), &salt)
        .map_err(|_| AppError::Internal)?
        .to_string();

    // Vérifie si l'utilisateur existe déjà
    if let Some(existing) = User::find()
        .filter(Column::Email.eq(payload.email.clone()))
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
    {
        return Ok((StatusCode::CREATED, Json(UserDto::from(existing))));
    }

    let new_user = ActiveUser {
        email: Set(payload.email.clone()),
        username: Set(payload.username.clone()),
        password_hash: Set(hash_str),
        is_admin: Set(false),
        ..Default::default()
    };
    let inserted = new_user.insert(db).await.map_err(|_| AppError::Internal)?;

    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let token = generate_jwt(inserted.id, inserted.is_admin, &jwt_secret)
        .map_err(|_| AppError::Internal)?;

    let mut cookie = Cookie::new("auth_token", token);
    cookie.set_path("/api");
    cookie.set_http_only(false);
    cookie.set_secure(false);
    jar.add(cookie);

    Ok((StatusCode::CREATED, Json(UserDto::from(inserted))))
}

#[handler]
pub async fn me(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
) -> PoemResult<(StatusCode, Json<UserDto>)> {
    let user = User::find_by_id(auth.user_id())
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::Unauthorized)?;

    Ok((StatusCode::OK, Json(UserDto::from(user))))
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
    jar.remove("auth_token");
    Ok(())
}
