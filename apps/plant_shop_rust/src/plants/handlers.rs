/// Handlers Poem pour gestion des plantes
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sqlx::PgPool;
use uuid::Uuid;
use crate::errors::AppError;
use super::models::{Plant, NewPlant};

#[handler]
pub async fn create_plant(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<NewPlant>,
) -> PoemResult<Json<Plant>, AppError> {
	let plant = sqlx::query_as!(
		Plant,
		"INSERT INTO plants (name, description, price, stock) VALUES ($1, $2, $3, $4) RETURNING *",
		payload.name,
		payload.description,
		payload.price,
		payload.stock
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::Conflict)?;
	Ok(Json(plant))
}

#[handler]
pub async fn list_plants(
	Data(pool): Data<&PgPool>
) -> PoemResult<Json<Vec<Plant>>, AppError> {
	let plants = sqlx::query_as!(
		Plant,
		"SELECT * FROM plants"
	)
	.fetch_all(pool)
	.await
	.map_err(|_| AppError::Internal)?;
	Ok(Json(plants))
}

#[handler]
pub async fn get_plant(
	Data(pool): Data<&PgPool>,
	Path(plant_id): Path<Uuid>,
) -> PoemResult<Json<Plant>, AppError> {
	let plant = sqlx::query_as!(
		Plant,
		"SELECT * FROM plants WHERE id = $1",
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
	Path(plant_id): Path<Uuid>,
	Json(payload): Json<NewPlant>,
) -> PoemResult<Json<Plant>, AppError> {
	let plant = sqlx::query_as!(
		Plant,
		"UPDATE plants SET
			name = $1,
			description = $2,
			price = $3,
			stock = $4
		 WHERE id = $5
		 RETURNING *",
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
	Path(plant_id): Path<Uuid>,
) -> PoemResult<(), AppError> {
	sqlx::query!("DELETE FROM plants WHERE id = $1", plant_id)
		.execute(pool)
		.await
		.map_err(|_| AppError::NotFound)?;
	Ok(())
}
