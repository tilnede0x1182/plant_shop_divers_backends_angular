/// Handlers Poem pour auth (login, register, me, logout)
use poem::{handler, web::{Json, Data}, Result as PoemResult};
use poem::web::cookie::{CookieJar, Cookie};
use sqlx::PgPool;
use crate::errors::AppError;
use super::models::{LoginPayload, RegisterPayload, UserAuth};
use super::jwt::{generate_jwt, verify_jwt};
use bcrypt::{verify, hash};
use poem::http::StatusCode;

#[handler]
pub async fn login(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<LoginPayload>,
	jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserAuth>)> {
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"SELECT id,email, username, password_hash, is_admin, created_at FROM users WHERE email = $1",
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
	let bcrypt_cost = std::env::var("BCRYPT_COST")
		.and_then(|v| v.parse::<u32>().map_err(|_| std::env::VarError::NotPresent))
		.unwrap_or(12);

	let hash_str = hash(&payload.password, bcrypt_cost).map_err(|_| AppError::Internal)?;

	// ✅ Vérifie si l'utilisateur existe déjà
	if let Some(existing) = sqlx::query_as!(
		UserAuth,
		"SELECT id, email, username, password_hash, is_admin, created_at
		FROM users WHERE email = $1",
		payload.email
	)
	.fetch_optional(pool)
	.await
	.map_err(AppError::DatabaseError)?
	{
		return Ok((StatusCode::CREATED, Json(existing)));
	}

	// 🧩 Création d’un nouvel utilisateur
	let user: UserAuth = sqlx::query_as!(
		UserAuth,
		"INSERT INTO users (email, username, password_hash)
		 VALUES ($1, $2, $3)
		 RETURNING id, email, username, password_hash, is_admin, created_at",
		payload.email,
		payload.name,
		hash_str
	)
	.fetch_one(pool)
	.await
	.map_err(|e| {
		if e.as_database_error().map_or(false, |db| db.is_unique_violation()) {
			AppError::Conflict
		} else {
			AppError::DatabaseError(e)
		}
	})?;

	// 🔐 Génère le cookie JWT
	let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let token = generate_jwt(user.id, user.is_admin, &jwt_secret).map_err(|_| AppError::Internal)?;

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
	jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<AuthMeResponse>)> {
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;

	let user = sqlx::query!(
		"SELECT id, email, username, is_admin FROM users WHERE id = $1",
		claims.sub
	)
	.fetch_one(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	let response = AuthMeResponse {
		id: user.id,
		email: user.email,
		name: user.username,
		admin: user.is_admin,
	};

	Ok((StatusCode::OK, Json(response)))
}

#[derive(serde::Serialize)]
struct AuthMeResponse {
	id: i32,
	email: String,
	name: String,
	admin: bool,
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
	jar.remove("auth_token");
	Ok(())
}
