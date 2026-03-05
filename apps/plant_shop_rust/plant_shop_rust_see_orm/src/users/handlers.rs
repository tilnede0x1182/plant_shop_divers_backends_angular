//! Handlers Poem pour gestion des utilisateurs.

// ==============================================================================
// Importations
// ==============================================================================

use crate::auth::session::{AdminGuard, AuthSession};
use crate::config::env_u64;
use crate::entity::users::{ActiveModel as ActiveUser, Column, Entity as User};
use crate::errors::AppError;
use crate::users::helpers::apply_user_updates;
use crate::users::models::{NewUser, UpdateUser, User as UserDto};
use argon2::password_hash::{rand_core::OsRng, SaltString};
use argon2::{Argon2, PasswordHasher};
use once_cell::sync::Lazy;
use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sea_orm::{
    ActiveModelTrait, DatabaseConnection, EntityTrait, IntoActiveModel, PaginatorTrait, QueryOrder,
    Set,
};

// ==============================================================================
// Constantes
// ==============================================================================

static ARGON2: Lazy<Argon2> = Lazy::new(Argon2::default);

// ==============================================================================
// Handlers
// ==============================================================================

/// Liste tous les utilisateurs (admin uniquement).
///
/// @param db Connection a la base de donnees
/// @param admin Guard verifiant les droits admin
/// @return Json<Vec<UserDto>> ou erreur
#[handler]
pub async fn list_users(
    Data(db): Data<&DatabaseConnection>,
    admin: AdminGuard,
) -> Result<Json<Vec<UserDto>>, AppError> {
    let _ = admin.user_id();
    // Récupère tous les utilisateurs (tri alphabétique par username)
    let page_size = env_u64("USERS_PAGE_SIZE", 128).max(1);
    let paginator = User::find()
        .order_by_asc(Column::Username)
        .paginate(db, page_size);
    let total_pages = paginator
        .num_pages()
        .await
        .map_err(|_| AppError::Internal)?;

    let mut mapped = Vec::new();
    for page in 0..total_pages {
        let chunk = paginator
            .fetch_page(page)
            .await
            .map_err(|_| AppError::Internal)?;
        mapped.extend(chunk.into_iter().map(UserDto::from));
    }

    Ok(Json(mapped))
}

/// Cree un nouvel utilisateur.
///
/// @param db Connection a la base de donnees
/// @param payload Donnees du nouvel utilisateur
/// @return Tuple (StatusCode, Json<UserDto>) ou erreur
#[handler]
pub async fn create_user(
    Data(db): Data<&DatabaseConnection>,
    Json(payload): Json<NewUser>,
) -> PoemResult<(StatusCode, Json<UserDto>)> {
    let salt = SaltString::generate(&mut OsRng);
    let password_hash = ARGON2
        .hash_password(payload.password.as_bytes(), &salt)
        .map_err(|_| AppError::Internal)?
        .to_string();

    let new_user = ActiveUser {
        username: Set(payload.name.clone()),
        email: Set(payload.email.clone()),
        password_hash: Set(password_hash),
        is_admin: Set(false),
        ..Default::default()
    };

    let inserted = new_user.insert(db).await.map_err(|_| AppError::Internal)?;
    Ok((StatusCode::CREATED, Json(UserDto::from(inserted))))
}

/// Recupere un utilisateur par ID.
///
/// @param db Connection a la base de donnees
/// @param user_id ID de l'utilisateur
/// @return Json<UserDto> ou erreur 404
#[handler]
pub async fn get_user(
    Data(db): Data<&DatabaseConnection>,
    Path(user_id): Path<i32>,
) -> PoemResult<Json<UserDto>> {
    let user = User::find_by_id(user_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    Ok(Json(UserDto::from(user)))
}

/// Met a jour un utilisateur.
///
/// @param auth Session d'authentification
/// @param db Connection a la base de donnees
/// @param user_id ID de l'utilisateur a modifier
/// @param payload Champs a mettre a jour
/// @return Json<UserDto> mis a jour ou erreur
#[handler]
pub async fn update_user(
    auth: AuthSession,
    Data(db): Data<&DatabaseConnection>,
    Path(user_id): Path<i32>,
    Json(payload): Json<UpdateUser>,
) -> PoemResult<Json<UserDto>> {
    let current = User::find_by_id(auth.user_id())
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::Unauthorized)?;

    if !current.is_admin && current.id != user_id {
        return Err(AppError::Forbidden.into());
    }

    let existing = User::find_by_id(user_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    let mut active = existing.into_active_model();
    apply_user_updates(&mut active, &payload, current.is_admin);

    let updated = active.update(db).await.map_err(|_| AppError::Internal)?;
    Ok(Json(UserDto::from(updated)))
}

/// Supprime un utilisateur.
///
/// @param db Connection a la base de donnees
/// @param user_id ID de l'utilisateur a supprimer
/// @return () ou erreur
#[handler]
pub async fn delete_user(
    Data(db): Data<&DatabaseConnection>,
    Path(user_id): Path<i32>,
) -> PoemResult<()> {
    User::delete_by_id(user_id)
        .exec(db)
        .await
        .map_err(|_| AppError::Internal)?;
    Ok(())
}
