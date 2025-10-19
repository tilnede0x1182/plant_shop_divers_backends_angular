/// Handlers Poem pour gestion utilisateurs (SeaORM)
use poem::{
	handler,
	web::{Data, Json, Path},
	Result as PoemResult,
	http::StatusCode,
	web::cookie::CookieJar,
};
use crate::errors::AppError;
use crate::auth::jwt::verify_jwt;
use crate::entity::users::{Entity as User, ActiveModel as ActiveUser, Model as UserModel, Column};
use sea_orm::{DatabaseConnection, Set, ActiveModelTrait, EntityTrait, QueryOrder, IntoActiveModel};
use bcrypt;
use serde::Deserialize;
use crate::users::models::User as UserDto;

/// DTO pour création d'utilisateur
#[derive(Deserialize)]
pub struct CreateUserDto {
	pub email: String,
	#[serde(alias = "name")]
	pub username: String,
	pub password: String,
}

/// DTO pour update utilisateur (tous champs optionnels sauf id dans l’URL)
#[derive(Deserialize)]
pub struct UpdateUserDto {
	#[serde(alias = "name")]
	pub username: Option<String>,
	pub email: Option<String>,
	pub is_admin: Option<bool>,
}

#[handler]
pub async fn list_users(Data(db): Data<&DatabaseConnection>, jar: &CookieJar) -> Result<Json<Vec<UserModel>>, AppError> {
	// Authentification JWT
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;

	// Vérifie que l'utilisateur courant est admin
	let current = User::find_by_id(claims.sub)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::Unauthorized)?;

	if !current.is_admin {
		return Err(AppError::Forbidden);
	}

	// Récupère tous les utilisateurs (tri alphabétique par username)
	let users = User::find()
		.order_by_asc(Column::Username)
		.all(db)
		.await
		.map_err(|_| AppError::Internal)?;

	Ok(Json(users))
}

#[handler]
pub async fn create_user(Data(db): Data<&DatabaseConnection>, Json(payload): Json<CreateUserDto>) -> PoemResult<(StatusCode, Json<UserModel>)> {
	let bcrypt_cost = std::env::var("BCRYPT_COST").unwrap_or("12".to_string()).parse::<u32>().unwrap_or(12);
	let password_hash = bcrypt::hash(payload.password.clone(), bcrypt_cost).map_err(|_| AppError::Internal)?;

	let new_user = ActiveUser {
		username: Set(payload.username.clone()),
		email: Set(payload.email.clone()),
		password_hash: Set(password_hash),
		is_admin: Set(false),
		..Default::default()
	};

	let inserted = new_user.insert(db).await.map_err(|_| AppError::Internal)?;
	Ok((StatusCode::CREATED, Json(inserted)))
}

#[handler]
pub async fn get_user(Data(db): Data<&DatabaseConnection>, Path(user_id): Path<i32>) -> PoemResult<Json<UserDto>> {
	let user = User::find_by_id(user_id)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::NotFound)?;

	let dto = UserDto {
		id: user.id,
		email: user.email,
		username: user.username,
		is_admin: user.is_admin,
		created_at: user.created_at.into(),
	};

	Ok(Json(dto))
}

#[handler]
pub async fn update_user(
	jar: &CookieJar,
	Data(db): Data<&DatabaseConnection>,
	Path(user_id): Path<i32>,
	Json(payload): Json<UpdateUserDto>,
) -> PoemResult<Json<UserModel>> {
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;

	let current = User::find_by_id(claims.sub)
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
	if let Some(name) = payload.username.clone() {
		active.username = Set(name);
	}
	if let Some(email) = payload.email.clone() {
		active.email = Set(email);
	}
	if let Some(is_admin) = payload.is_admin {
		if current.is_admin {
			active.is_admin = Set(is_admin);
		}
	}

	let updated = active.update(db).await.map_err(|_| AppError::Internal)?;
	Ok(Json(updated))
}

#[handler]
pub async fn delete_user(Data(db): Data<&DatabaseConnection>, Path(user_id): Path<i32>) -> PoemResult<()> {
	User::delete_by_id(user_id)
		.exec(db)
		.await
		.map_err(|_| AppError::Internal)?;
	Ok(())
}
