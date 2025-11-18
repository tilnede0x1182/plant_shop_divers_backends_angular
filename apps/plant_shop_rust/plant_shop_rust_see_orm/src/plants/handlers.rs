use crate::auth::session::AdminGuard;
use crate::entity::plants::{
    ActiveModel as ActivePlant, Column, Entity as Plant, Model as PlantModel,
};
use crate::errors::AppError;
use crate::plants::helpers::apply_plant_updates;
use crate::plants::models::{NewPlant, UpdatePlant};
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

/// Création d’une plante (201 Created)
#[handler]
pub async fn create_plant(
    Data(db): Data<&DatabaseConnection>,
    admin: AdminGuard,
    Json(payload): Json<NewPlant>,
) -> PoemResult<(StatusCode, Json<PlantModel>), AppError> {
    let _ = admin.user_id();
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
    Json(payload): Json<UpdatePlant>,
) -> PoemResult<Json<PlantModel>> {
    let existing = Plant::find_by_id(plant_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    let mut active = existing.into_active_model();
    apply_plant_updates(&mut active, &payload);

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
