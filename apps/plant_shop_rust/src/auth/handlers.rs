/// Handlers Poem pour auth (login, register, me, logout)
use poem::{handler, web::{Json, Data}, Result as PoemResult, IntoResponse};
use poem::web::cookie::{CookieJar, Cookie};
use sqlx::PgPool;
use crate::errors::AppError;
use super::models::{AuthPayload, UserAuth};
use super::jwt::{generate_jwt, verify_jwt};
use bcrypt::{verify, hash};

#[handler]
pub async fn login(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<AuthPayload>,
	jar: &CookieJar,
) -> PoemResult<Json<UserAuth>> {
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"SELECT id, email, username, password_hash, is_admin, created_at FROM users WHERE email = $1",
		payload.email
	)
	.fetch_optional(pool)
	.await.map_err(|e| AppError::DatabaseError(e))?

	.ok_or(AppError::Unauthorized)?;

	if !verify(&payload.password, &user.password_hash).unwrap_or(false) {
		return Err(AppError::Unauthorized.into());
	}

	let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let jwt = generate_jwt(user.id, user.is_admin, &jwt_secret).map_err(|_| AppError::Internal)?;

	jar.add(Cookie::new("auth_token", jwt));

	Ok(Json(user))
}

#[handler]
pub async fn register(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<AuthPayload>,
) -> PoemResult<Json<UserAuth>> {
    let bcrypt_cost = std::env::var("BCRYPT_COST")
        .and_then(|s| s.parse::<u32>().map_err(|_| std::env::VarError::NotPresent))
        .unwrap_or(12);

	let hash_str = hash(&payload.password, bcrypt_cost).map_err(|_| AppError::Internal)?;
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"INSERT INTO users (email, username, password_hash) VALUES ($1, $2, $3) RETURNING *",
		payload.email,
		payload.email,
		hash_str
	)
	.fetch_one(pool)
	.await
	.map_err(|e| {
        if e.as_database_error().map_or(false, |db_err| db_err.is_unique_violation()) {
            AppError::Conflict
        } else {
            AppError::DatabaseError(e)
        }
    })?;
	Ok(Json(user))
}

#[handler]
pub async fn me(
	Data(pool): Data<&PgPool>,
	jar: &CookieJar,
) -> PoemResult<Json<UserAuth>> {
	let token = jar.get("auth_token").map(|c| c.value_str().to_string()).ok_or(AppError::Unauthorized)?;
	let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;

	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"SELECT id, email, username, password_hash, is_admin, created_at FROM users WHERE id = $1",
		claims.sub
	)
	.fetch_one(pool)
	.await.map_err(|e| AppError::DatabaseError(e))?
;

	Ok(Json(user))
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
	jar.remove("auth_token");
	Ok(())
}
