use super::models::{NewUser, UpdateUser, User};
use crate::auth::session::{AdminGuard, AuthSession};
use crate::db::updates::PartialUpdate;
use crate::dto::UserResponse;
use crate::errors::AppError;
use crate::response::buffered_json;
use crate::state::AppState;
use argon2::password_hash::{rand_core::OsRng, SaltString};
use argon2::{Argon2, PasswordHasher};
use poem::http::StatusCode;
/// Handlers Poem pour gestion utilisateurs
use poem::{
    handler,
    web::{Data, Json, Path},
    Response, Result as PoemResult,
};

#[handler]
pub async fn list_users(
    Data(state): Data<&AppState>,
    admin: AdminGuard,
) -> Result<Response, AppError> {
    let _ = admin.user_id();

    if let Some(cached) = state.user_cache().get().await {
        return buffered_json(&cached, StatusCode::OK);
    }

    let users = sqlx::query_as!(
        User,
        r#"SELECT id, email, username, is_admin, created_at
            FROM users
            ORDER BY is_admin DESC, username COLLATE "und-x-icu" ASC"#
    )
    .fetch_all(state.read_pool())
    .await
    .map_err(AppError::DatabaseError)?;

    let responses: Vec<UserResponse> = users.into_iter().map(UserResponse::from).collect();
    state.user_cache().set(&responses).await;

    buffered_json(&responses, StatusCode::OK)
}

#[handler]
pub async fn create_user(
    Data(state): Data<&AppState>,
    Json(payload): Json<NewUser>,
) -> PoemResult<(StatusCode, Json<UserResponse>)> {
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
    .fetch_one(state.write_pool())
    .await
    .map_err(|e| AppError::DatabaseError(e))?;

    state.user_cache().invalidate().await;

    Ok((StatusCode::CREATED, Json(UserResponse::from(user))))
}

#[handler]
pub async fn get_user(
    Data(state): Data<&AppState>,
    Path(user_id): Path<i32>,
) -> PoemResult<Json<UserResponse>> {
    let user = sqlx::query_as!(
        User,
        "SELECT id,email, username, is_admin, created_at FROM users WHERE id = $1",
        user_id
    )
    .fetch_one(state.read_pool())
    .await
    .map_err(|_| AppError::NotFound)?;
    Ok(Json(UserResponse::from(user)))
}

#[handler]
pub async fn update_user(
    auth: AuthSession,
    Data(state): Data<&AppState>,
    Path(user_id): Path<i32>,
    Json(payload): Json<UpdateUser>,
) -> PoemResult<Json<UserResponse>> {
    let current = sqlx::query!(
        r#"SELECT id, is_admin FROM users WHERE id = $1"#,
        auth.user_id()
    )
    .fetch_one(state.read_pool())
    .await
    .map_err(AppError::DatabaseError)?;

    let admin_value = if current.is_admin {
        payload.admin
    } else {
        None
    };

    if !current.is_admin && current.id != user_id {
        return Err(AppError::Forbidden.into());
    }

    let mut updater = PartialUpdate::new("users");
    updater.set_with_coalesce("username", payload.name.clone());
    updater.set_with_coalesce("email", payload.email.clone());
    updater.set_with_coalesce("is_admin", admin_value);

    let mut builder = updater.finish();
    builder.push(" WHERE id = ");
    builder.push_bind(user_id);
    builder.push(" RETURNING id, email, username, is_admin, created_at");

    let user = builder
        .build_query_as::<User>()
        .fetch_one(state.write_pool())
        .await
        .map_err(AppError::DatabaseError)?;

    state.user_cache().invalidate().await;

    Ok(Json(UserResponse::from(user)))
}

#[handler]
pub async fn delete_user(Data(state): Data<&AppState>, Path(user_id): Path<i32>) -> PoemResult<()> {
    let result = sqlx::query!("DELETE FROM users WHERE id = $1", user_id)
        .execute(state.write_pool())
        .await
        .map_err(|e| AppError::DatabaseError(e))?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound.into());
    }

    state.user_cache().invalidate().await;
    Ok(())
}
