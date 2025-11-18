use super::models::{NewPlant, Plant, UpdatePlant};
use crate::auth::session::AdminGuard;
use crate::db::updates::PartialUpdate;
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
    _admin: AdminGuard,
    Json(payload): Json<NewPlant>,
) -> PoemResult<(StatusCode, Json<Plant>), AppError> {
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
    let mut updater = PartialUpdate::new("plants");
    updater.set_with_coalesce("name", payload.name.clone());
    updater.set_with_coalesce("description", payload.description.clone());
    updater.set_with_coalesce("price", payload.price.clone());
    updater.set_with_coalesce("stock", payload.stock);

    let mut builder = updater.finish();
    builder.push(" WHERE id = ");
    builder.push_bind(plant_id);
    builder.push(" RETURNING id, name, description, price, stock, created_at");

    let plant = builder
        .build_query_as::<Plant>()
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
