/// Handlers Poem pour gestion des plantes
use poem::{handler, web::{Data, Json, Path, cookie::CookieJar}, http::StatusCode, Result as PoemResult};
use crate::auth::jwt::verify_jwt;
use sqlx::PgPool;
use crate::errors::AppError;
use super::models::{Plant, NewPlant, UpdatePlant};

/// Création d’une plante (201 Created)
/// @payload données de la plante
/// @return plante créée
#[handler]
pub async fn create_plant(
	Data(pool): Data<&PgPool>,
	jar: &CookieJar,
	Json(payload): Json<NewPlant>,
) -> PoemResult<(StatusCode, Json<Plant>), AppError> {
	// vérifie si l’utilisateur est admin
	let token = jar.get("auth_token").map(|c| c.value_str().to_string()).ok_or(AppError::Unauthorized)?;
	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;
	if !claims.is_admin {
		return Err(AppError::Forbidden.into()); // 403
	}
	let plant = sqlx::query_as!(
		Plant,
		"INSERT INTO plants (name, description, price, stock) VALUES ($1, $2, $3, $4) RETURNING id, name, description, price, stock, created_at",
		payload.name,
		payload.description,
		payload.price,
		payload.stock
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::Conflict)?;
	Ok((StatusCode::CREATED, Json(plant)))
}

#[handler]
pub async fn list_plants(
	Data(pool): Data<&PgPool>
) -> PoemResult<Json<Vec<Plant>>, AppError> {
	let plants = sqlx::query_as!(
		Plant,
		"SELECT id, name, description, price, stock, created_at FROM plants ORDER BY name ASC",
	)
	.fetch_all(pool)
	.await
	.map_err(|_| AppError::Internal)?;
	Ok(Json(plants))
}

#[handler]
pub async fn get_plant(
    Data(pool): Data<&PgPool>,
    Path(plant_id): Path<i32>,
) -> PoemResult<Json<Plant>> {
	let plant = sqlx::query_as!(
		Plant,
		"SELECT id, name, description, price, stock, created_at
 		FROM plants WHERE id = $1",
		plant_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(plant))
}

#[handler]
pub async fn update_plant(
    Data(pool): Data<&PgPool>,
    Path(plant_id): Path<i32>,
    Json(payload): Json<UpdatePlant>,
) -> PoemResult<Json<Plant>> {
	let plant = sqlx::query_as!(
		Plant,
		"UPDATE plants SET
			name = COALESCE($1, name),
			description = COALESCE($2, description),
			price = COALESCE($3, price),
			stock = COALESCE($4, stock)
		 WHERE id = $5
		 RETURNING id, name, description, price, stock, created_at",
		payload.name,
		payload.description,
		payload.price,
		payload.stock,
		plant_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(plant))
}

#[handler]
pub async fn delete_plant(
    Data(pool): Data<&PgPool>,
    Path(plant_id): Path<i32>,
) -> PoemResult<()> {
	sqlx::query!("DELETE FROM plants WHERE id = $1", plant_id)
		.execute(pool)
		.await
		.map_err(|_| AppError::NotFound)?;
	Ok(())
}
