/// Handlers Poem pour gestion utilisateurs
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use poem::middleware::AddData;
use poem::Request;
use sqlx::PgPool;
use crate::errors::AppError;
use super::models::{User, UpdateUser, NewUser};
use poem::http::StatusCode;
use poem::web::cookie::CookieJar;
use crate::auth::jwt::verify_jwt;

#[handler]
pub async fn list_users(Data(pool): Data<&PgPool>, jar: &CookieJar) -> Result<Json<Vec<User>>, AppError> {
	// Extraction du token JWT depuis le cookie
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;

	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;

	// Vérification du rôle admin
	let user = sqlx::query!(
		"SELECT id, is_admin FROM users WHERE id = $1",
		claims.sub
	)
	.fetch_one(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	if !user.is_admin {
		return Err(AppError::Forbidden);
	}

	// Récupération de tous les utilisateurs
	let users = sqlx::query_as!(
		User,
		r#"SELECT id, email, username, is_admin, created_at
			FROM users
			ORDER BY is_admin DESC, username COLLATE "und-x-icu" ASC"#
	)
	.fetch_all(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	Ok(Json(users))
}

#[handler]
pub async fn create_user(
    Data(pool): Data<&PgPool>,
    Json(payload): Json<NewUser>,
) -> PoemResult<(StatusCode, Json<User>)> {
    let bcrypt_cost = std::env::var("BCRYPT_COST").unwrap_or("12".to_string()).parse::<u32>().unwrap_or(12);
    let password_hash = bcrypt::hash(payload.password, bcrypt_cost).map_err(|_| AppError::Internal)?;

    let user = sqlx::query_as!(
        User,
        "INSERT INTO users (username, email, password_hash) VALUES ($1, $2, $3) RETURNING id,email, username, is_admin, created_at",
        payload.name,
        payload.email,
        password_hash
    )
    .fetch_one(pool)
    .await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok((StatusCode::CREATED, Json(user)))
}

#[handler]
pub async fn get_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<i32>,
) -> PoemResult<Json<User>> {
	let user = sqlx::query_as!(
		User,
		"SELECT id,email, username, is_admin, created_at FROM users WHERE id = $1",
		user_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(user))
}

#[handler]
pub async fn update_user(
	jar: &CookieJar,
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<i32>,
	Json(payload): Json<UpdateUser>,
) -> PoemResult<Json<User>> {
	// Auth + utilisateur courant depuis le JWT
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;

	// Charger rôle courant (source de vérité DB)
	let current = sqlx::query!(
		r#"SELECT id, is_admin FROM users WHERE id = $1"#,
		claims.sub
	)
	.fetch_one(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	// Ignorer la tentative d’élévation de privilèges par un non-admin
	let admin_value = if current.is_admin {
			payload.admin
	} else {
			None
	};

	// Interdire édition d'un autre user si non-admin
	if !current.is_admin && current.id != user_id {
		return Err(AppError::Forbidden.into());
	}

	let user = sqlx::query_as!(
		User,
		r#"UPDATE users SET
			username = COALESCE($1, username),
			email    = COALESCE($2, email),
			is_admin = COALESCE($3, is_admin)
		WHERE id = $4
		RETURNING id, email, username, is_admin, created_at"#,
		payload.name,
		payload.email,
		admin_value,
		user_id
	)
	.fetch_one(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	Ok(Json(user))
}

#[handler]
pub async fn delete_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<i32>,
) -> PoemResult<()> {
	let result = sqlx::query!("DELETE FROM users WHERE id = $1", user_id)
		.execute(pool)
		.await
		.map_err(|e| AppError::DatabaseError(e))?;

	if result.rows_affected() == 0 {
		return Err(AppError::NotFound.into());
	}
	Ok(())
}
