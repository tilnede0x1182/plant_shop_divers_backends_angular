use super::models::{NewUser, UpdateUser, User};
use crate::auth::session::AuthSession;
use crate::errors::AppError;
use argon2::password_hash::{rand_core::OsRng, SaltString};
use argon2::{Argon2, PasswordHasher};
use poem::http::StatusCode;
/// Handlers Poem pour gestion utilisateurs
use poem::{
    handler,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sqlx::PgPool;

#[handler]
pub async fn list_users(
    Data(pool): Data<&PgPool>,
    auth: AuthSession,
) -> Result<Json<Vec<User>>, AppError> {
    // Vérification du rôle admin
    let user = sqlx::query!("SELECT id, is_admin FROM users WHERE id = $1", auth.user_id())
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
    let salt = SaltString::generate(&mut OsRng);
    let password_hash = Argon2::default()
        .hash_password(payload.password.as_bytes(), &salt)
        .map_err(|_| AppError::Internal)?
        .to_string();

    let user = sqlx::query_as!(
		User,
		"INSERT INTO users (username, email, password_hash) VALUES ($1, $2, $3) RETURNING id,email, username, is_admin, created_at",
		payload.name,
		payload.email,
		password_hash
	)
	.fetch_one(pool)
	.await
	.map_err(|e| AppError::DatabaseError(e))?;

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
    auth: AuthSession,
    Data(pool): Data<&PgPool>,
    Path(user_id): Path<i32>,
    Json(payload): Json<UpdateUser>,
) -> PoemResult<Json<User>> {
    // Charger rôle courant (source de vérité DB)
    let current = sqlx::query!(
        r#"SELECT id, is_admin FROM users WHERE id = $1"#,
        auth.user_id()
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
pub async fn delete_user(Data(pool): Data<&PgPool>, Path(user_id): Path<i32>) -> PoemResult<()> {
    let result = sqlx::query!("DELETE FROM users WHERE id = $1", user_id)
        .execute(pool)
        .await
        .map_err(|e| AppError::DatabaseError(e))?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound.into());
    }
    Ok(())
}
