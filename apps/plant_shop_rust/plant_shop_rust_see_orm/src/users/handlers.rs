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
use crate::entity::users::{self, Entity as User, ActiveModel as ActiveUser, Model as UserModel, Column};
use sea_orm::{DatabaseConnection, Set, ActiveModelTrait, EntityTrait, QueryFilter};
use poem::error::InternalServerError;

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
		.map_err(|_| AppError::DatabaseError(anyhow::anyhow!("Erreur requête")))?
		.ok_or(AppError::Unauthorized)?;

	if !current.is_admin {
		return Err(AppError::Forbidden);
	}

	// Récupère tous les utilisateurs
	let users = User::find()
		.order_by_asc(Column::Username)
		.all(db)
		.await
		.map_err(|_| AppError::Internal)?;

	Ok(Json(users))
}

#[handler]
pub async fn create_user(Data(db): Data<&DatabaseConnection>, Json(payload): Json<users::Model>) -> PoemResult<(StatusCode, Json<UserModel>)> {
	let bcrypt_cost = std::env::var("BCRYPT_COST").unwrap_or("12".to_string()).parse::<u32>().unwrap_or(12);
	let password_hash = bcrypt::hash(payload.password_hash.clone(), bcrypt_cost).map_err(|_| AppError::Internal)?;

	let new_user = ActiveUser {
		username: Set(payload.username.clone()),
		email: Set(payload.email.clone()),
		password_hash: Set(password_hash),
		..Default::default()
	};

	let inserted = new_user.insert(db).await.map_err(|_| InternalServerError("Erreur d’insertion utilisateur"))?;
	Ok((StatusCode::CREATED, Json(inserted)))
}

#[handler]
pub async fn get_user(Data(db): Data<&DatabaseConnection>, Path(user_id): Path<i32>) -> PoemResult<Json<UserModel>> {
	let user = User::find_by_id(user_id)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::NotFound)?;
	Ok(Json(user))
}

#[handler]
pub async fn update_user(
	jar: &CookieJar,
	Data(db): Data<&DatabaseConnection>,
	Path(user_id): Path<i32>,
	Json(payload): Json<users::Model>,
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

	let mut existing = User::find_by_id(user_id)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::NotFound)?;

	if let Some(name) = payload.username.clone() {
		existing.username = name;
	}
	if let Some(email) = payload.email.clone() {
		existing.email = email;
	}
	if current.is_admin {
		existing.is_admin = payload.is_admin;
	}

	let updated = existing.into_active_model().update(db).await.map_err(|_| AppError::Internal)?;
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
