use crate::auth::session::AuthSession;
use crate::entity::plants::{
    ActiveModel as ActivePlant, Column, Entity as Plant, Model as PlantModel,
};
use crate::errors::AppError;
/// Handlers Poem pour gestion des plantes (version SeaORM)
use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sea_orm::{
    ActiveModelTrait, DatabaseConnection, EntityTrait, IntoActiveModel, QueryOrder, Set,
};
use serde::Deserialize;

/// DTO création plante
#[derive(Deserialize)]
pub struct CreatePlantDto {
    pub name: String,
    pub description: Option<String>,
    pub price: i32,
    pub stock: i32,
}

/// DTO update plante (tous champs optionnels)
#[derive(Deserialize)]
pub struct UpdatePlantDto {
    pub name: Option<String>,
    pub description: Option<String>,
    pub price: Option<i32>,
    pub stock: Option<i32>,
}

/// Création d’une plante (201 Created)
#[handler]
pub async fn create_plant(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
    Json(payload): Json<CreatePlantDto>,
) -> PoemResult<(StatusCode, Json<PlantModel>), AppError> {
    if !auth.is_admin() {
        return Err(AppError::Forbidden.into());
    }

    // Insertion via ActiveModel SeaORM
    let new_plant = ActivePlant {
        name: Set(payload.name.clone()),
        description: Set(payload.description.clone()),
        price: Set(payload.price),
        stock: Set(payload.stock),
        ..Default::default()
    };

    let inserted = new_plant.insert(db).await.map_err(|e| {
        println!("❌ ERREUR d’insertion plante: {e}");
        AppError::Internal
    })?;
    Ok((StatusCode::CREATED, Json(inserted)))
}

/// Liste des plantes (GET /plants)
#[handler]
pub async fn list_plants(
    Data(db): Data<&DatabaseConnection>,
) -> PoemResult<Json<Vec<PlantModel>>, AppError> {
    let plants = Plant::find()
        .order_by_asc(Column::Name)
        .all(db)
        .await
        .map_err(|_| AppError::Internal)?;
    Ok(Json(plants))
}

/// Lecture d’une plante par son ID
#[handler]
pub async fn get_plant(
    Data(db): Data<&DatabaseConnection>,
    Path(plant_id): Path<i32>,
) -> PoemResult<Json<PlantModel>> {
    let plant = Plant::find_by_id(plant_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;
    Ok(Json(plant))
}

/// Mise à jour d’une plante
#[handler]
pub async fn update_plant(
    Data(db): Data<&DatabaseConnection>,
    Path(plant_id): Path<i32>,
    Json(payload): Json<UpdatePlantDto>,
) -> PoemResult<Json<PlantModel>> {
    let existing = Plant::find_by_id(plant_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    let mut active = existing.into_active_model();
    if let Some(name) = payload.name.clone() {
        active.name = Set(name);
    }
    if let Some(desc) = payload.description.clone() {
        active.description = Set(Some(desc));
    }
    if let Some(price) = payload.price {
        active.price = Set(price);
    }
    if let Some(stock) = payload.stock {
        active.stock = Set(stock);
    }

    let updated = active.update(db).await.map_err(|_| AppError::Internal)?;
    Ok(Json(updated))
}

/// Suppression d’une plante
#[handler]
pub async fn delete_plant(
    Data(db): Data<&DatabaseConnection>,
    Path(plant_id): Path<i32>,
) -> PoemResult<()> {
    Plant::delete_by_id(plant_id)
        .exec(db)
        .await
        .map_err(|_| AppError::Internal)?;
    Ok(())
}
