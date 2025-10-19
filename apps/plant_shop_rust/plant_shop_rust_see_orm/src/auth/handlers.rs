/// Handlers Poem pour auth (login, register, me, logout) — version SeaORM

use poem::{
	handler,
	web::{Json, Data},
	web::cookie::{CookieJar, Cookie},
	http::StatusCode,
	Result as PoemResult,
};
use crate::errors::AppError;
use crate::auth::jwt::{generate_jwt, verify_jwt};
use crate::entity::users::{Entity as User, Model as UserModel, ActiveModel as ActiveUser, Column};
use sea_orm::{DatabaseConnection, EntityTrait, QueryFilter, Set, ActiveModelTrait, ColumnTrait};
use bcrypt::{verify, hash};

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
) -> PoemResult<(StatusCode, Json<UserModel>)> {
	let user = User::find()
		.filter(Column::Email.eq(payload.email.clone()))
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
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
	Data(db): Data<&DatabaseConnection>,
	Json(payload): Json<RegisterPayload>,
	jar: &CookieJar,
) -> PoemResult<(StatusCode, Json<UserModel>)> {
	let bcrypt_cost = std::env::var("BCRYPT_COST")
		.ok()
		.and_then(|v| v.parse::<u32>().ok())
		.unwrap_or(12);

	let hash_str = hash(&payload.password, bcrypt_cost).map_err(|_| AppError::Internal)?;

	// Vérifie si l'utilisateur existe déjà
	if let Some(existing) = User::find()
		.filter(Column::Email.eq(payload.email.clone()))
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
	{
		return Ok((StatusCode::CREATED, Json(existing)));
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
	let token = generate_jwt(inserted.id, inserted.is_admin, &jwt_secret).map_err(|_| AppError::Internal)?;

	let mut cookie = Cookie::new("auth_token", token);
	cookie.set_path("/api");
	cookie.set_http_only(false);
	cookie.set_secure(false);
	jar.add(cookie);

	Ok((StatusCode::CREATED, Json(inserted)))
}

#[derive(serde::Serialize)]
pub struct AuthMeResponse {
	pub id: i32,
	pub email: String,
	pub name: String,
	pub admin: bool,
}

#[handler]
pub async fn me(Data(db): Data<&DatabaseConnection>, jar: &CookieJar) -> PoemResult<(StatusCode, Json<AuthMeResponse>)> {
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;

	let user = User::find_by_id(claims.sub)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::Unauthorized)?;

	let response = AuthMeResponse {
		id: user.id,
		email: user.email,
		name: user.username,
		admin: user.is_admin,
	};

	Ok((StatusCode::OK, Json(response)))
}

#[handler]
pub async fn logout(jar: &CookieJar) -> PoemResult<()> {
	jar.remove("auth_token");
	Ok(())
}
