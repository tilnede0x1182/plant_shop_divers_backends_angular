/// Handlers Poem pour gestion utilisateurs
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sqlx::PgPool;
use uuid::Uuid;
use crate::errors::AppError;
use super::models::{User, UpdateUser, NewUser};
use poem::http::StatusCode;

#[handler]
pub async fn list_users(
	Data(pool): Data<&PgPool>,
) -> PoemResult<Json<Vec<User>>> {
    let users = sqlx::query_as!(
        User,
        "SELECT id, email, username, is_admin, created_at FROM users"
    )
    .fetch_all(pool)
    .await.map_err(|e| AppError::DatabaseError(e))?
;
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
        "INSERT INTO users (username, email, password_hash) VALUES ($1, $2, $3) RETURNING id, email, username, is_admin, created_at",
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
	Path(user_id): Path<Uuid>,
) -> PoemResult<(StatusCode, Json<User>)> {
	let user = sqlx::query_as!(
		User,
		"SELECT id, email, username, is_admin, created_at FROM users WHERE id = $1",
		user_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok((StatusCode::CREATED, Json(user)))
}

#[handler]
pub async fn update_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<Uuid>,
	Json(payload): Json<UpdateUser>,
) -> PoemResult<(StatusCode, Json<User>)> {
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
	.await.map_err(|e| AppError::DatabaseError(e))?
;
	Ok((StatusCode::CREATED, Json(user)))
}

#[handler]
pub async fn delete_user(
	Data(pool): Data<&PgPool>,
	Path(user_id): Path<Uuid>,
) -> PoemResult<()> {
	let result = sqlx::query!("DELETE FROM users WHERE id = $1", user_id)
		.execute(pool)
		.await.map_err(|e| AppError::DatabaseError(e))?
;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound.into());
    }
	Ok(())
}
