/// Handlers Poem pour gestion utilisateurs
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sqlx::PgPool;
use uuid::Uuid;
use crate::{errors::AppError};
use super::models::{User, UpdateUser};

#[handler]
pub async fn get_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<Uuid>,
) -> PoemResult<Json<User>, AppError> {
	let user = sqlx::query_as!(
		User,
		"SELECT id, email, username, is_admin, created_at FROM users WHERE id = $1",
		user_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(user))
}

#[handler]
pub async fn update_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<Uuid>,
	Json(payload): Json<UpdateUser>,
) -> PoemResult<Json<User>, AppError> {
	let user = sqlx::query_as!(
		User,
		"UPDATE users SET
			username = COALESCE($1, username),
			email = COALESCE($2, email)
		 WHERE id = $3
		 RETURNING id, email, username, is_admin, created_at",
		payload.username,
		payload.email,
		user_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(user))
}

#[handler]
pub async fn delete_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<Uuid>,
) -> PoemResult<(), AppError> {
	sqlx::query!("DELETE FROM users WHERE id = $1", user_id)
		.execute(pool)
		.await
		.map_err(|_| AppError::NotFound)?;
	Ok(())
}
