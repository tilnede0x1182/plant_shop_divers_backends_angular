use super::models::{NewPlant, Plant, UpdatePlant};
use crate::auth::session::AdminGuard;
use crate::db::updates::PartialUpdate;
use crate::dto::PlantResponse;
use crate::errors::AppError;
use crate::response::buffered_json;
use crate::state::AppState;
use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Response, Result as PoemResult,
};

/// Création d’une plante (201 Created)
#[handler]
pub async fn create_plant(
    Data(state): Data<&AppState>,
    admin: AdminGuard,
    Json(payload): Json<NewPlant>,
) -> PoemResult<(StatusCode, Json<PlantResponse>), AppError> {
    let _ = admin.user_id();
    let plant = sqlx::query_as!(
        Plant,
        "INSERT INTO plants (name, description, price, stock) VALUES ($1, $2, $3, $4) RETURNING id, name, description, price, stock, created_at",
        payload.name,
        payload.description,
        payload.price,
        payload.stock
    )
    .fetch_one(state.write_pool())
    .await
    .map_err(|_| AppError::Conflict)?;

    state.plant_cache().invalidate().await;
    Ok((StatusCode::CREATED, Json(PlantResponse::from(plant))))
}

#[handler]
pub async fn list_plants(Data(state): Data<&AppState>) -> Result<Response, AppError> {
    if let Some(cached) = state.plant_cache().get().await {
        return buffered_json(&cached, StatusCode::OK);
    }

    let plants = sqlx::query_as!(
        Plant,
        "SELECT id, name, description, price, stock, created_at FROM plants ORDER BY name ASC",
    )
    .fetch_all(state.read_pool())
    .await
    .map_err(|_| AppError::Internal)?;

    let response: Vec<PlantResponse> = plants.into_iter().map(PlantResponse::from).collect();
    state.plant_cache().set(&response).await;

    buffered_json(&response, StatusCode::OK)
}

#[handler]
pub async fn get_plant(
    Data(state): Data<&AppState>,
    Path(plant_id): Path<i32>,
) -> PoemResult<Json<PlantResponse>> {
    let plant = sqlx::query_as!(
        Plant,
        "SELECT id, name, description, price, stock, created_at
 		FROM plants WHERE id = $1",
        plant_id
    )
    .fetch_one(state.read_pool())
    .await
    .map_err(|_| AppError::NotFound)?;
    Ok(Json(PlantResponse::from(plant)))
}

#[handler]
pub async fn update_plant(
    Data(state): Data<&AppState>,
    Path(plant_id): Path<i32>,
    Json(payload): Json<UpdatePlant>,
) -> PoemResult<Json<PlantResponse>> {
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
        .fetch_one(state.write_pool())
        .await
        .map_err(|_| AppError::NotFound)?;

    state.plant_cache().invalidate().await;

    Ok(Json(PlantResponse::from(plant)))
}

#[handler]
pub async fn delete_plant(
    Data(state): Data<&AppState>,
    Path(plant_id): Path<i32>,
) -> PoemResult<()> {
    sqlx::query!("DELETE FROM plants WHERE id = $1", plant_id)
        .execute(state.write_pool())
        .await
        .map_err(|_| AppError::NotFound)?;

    state.plant_cache().invalidate().await;
    Ok(())
}
