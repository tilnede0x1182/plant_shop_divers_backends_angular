/// Handlers Poem pour auth (login, register, me, logout)
use poem::{handler, web::{Json, Data, CookieJar}, Result as PoemResult};
use sqlx::PgPool;
use crate::{config::Config, errors::AppError};
use super::models::{AuthPayload, UserAuth};
use super::jwt::{generate_jwt, verify_jwt};
use bcrypt::{verify, hash, DEFAULT_COST};

#[handler]
pub async fn login(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<AuthPayload>,
	CookieJar jar: CookieJar,
) -> PoemResult<Json<UserAuth>, AppError> {
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"SELECT * FROM users WHERE email = $1",
		payload.email
	)
	.fetch_optional(pool)
	.await
	.map_err(|_| AppError::Internal)?
	.ok_or(AppError::Unauthorized)?;
	if !verify(&payload.password, &user.password_hash).unwrap_or(false) {
		return Err(AppError::Unauthorized);
	}
	let jwt = generate_jwt(user.id, user.is_admin, &std::env::var("JWT_SECRET").unwrap()).map_err(|_| AppError::Internal)?;
	jar.add(("auth_token", jwt));
	Ok(Json(user))
}

#[handler]
pub async fn register(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<AuthPayload>,
) -> PoemResult<Json<UserAuth>, AppError> {
	let hash_str = hash(&payload.password, DEFAULT_COST).map_err(|_| AppError::Internal)?;
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"INSERT INTO users (email, username, password_hash) VALUES ($1, $2, $3) RETURNING *",
		payload.email,
		payload.email,
		hash_str
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::Conflict)?;
	Ok(Json(user))
}

#[handler]
pub async fn me(
	Data(pool): Data<&PgPool>,
	CookieJar jar: CookieJar,
) -> PoemResult<Json<UserAuth>, AppError> {
	let token = jar.get("auth_token").ok_or(AppError::Unauthorized)?.value();
	let claims = verify_jwt(token, &std::env::var("JWT_SECRET").unwrap()).map_err(|_| AppError::Unauthorized)?;
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"SELECT * FROM users WHERE id = $1",
		claims.sub
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(user))
}

#[handler]
pub async fn logout(CookieJar jar: CookieJar) -> PoemResult<(), AppError> {
	jar.remove("auth_token");
	Ok(())
}
