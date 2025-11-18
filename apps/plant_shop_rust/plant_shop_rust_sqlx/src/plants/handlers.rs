use super::models::{NewPlant, Plant, UpdatePlant};
use crate::auth::session::AuthSession;
use crate::errors::AppError;
/// Handlers Poem pour gestion des plantes
use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sqlx::PgPool;

/// Création d’une plante (201 Created)
/// @payload données de la plante
/// @return plante créée
#[handler]
pub async fn create_plant(
    Data(pool): Data<&PgPool>,
    auth: AuthSession,
    Json(payload): Json<NewPlant>,
) -> PoemResult<(StatusCode, Json<Plant>), AppError> {
    if !auth.is_admin() {
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
pub async fn list_plants(Data(pool): Data<&PgPool>) -> PoemResult<Json<Vec<Plant>>, AppError> {
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
pub async fn delete_plant(Data(pool): Data<&PgPool>, Path(plant_id): Path<i32>) -> PoemResult<()> {
    sqlx::query!("DELETE FROM plants WHERE id = $1", plant_id)
        .execute(pool)
        .await
        .map_err(|_| AppError::NotFound)?;
    Ok(())
}
