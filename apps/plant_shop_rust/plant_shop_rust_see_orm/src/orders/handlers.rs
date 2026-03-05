//! Handlers Poem pour gestion des commandes.

// ==============================================================================
// Importations
// ==============================================================================

use std::collections::HashMap;

use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sea_orm::{
    ActiveModelTrait, ColumnTrait, DatabaseConnection, EntityTrait, IntoActiveModel, QueryFilter,
    Set, TransactionTrait,
};
use serde::Deserialize;

use crate::auth::session::AuthSession;
use crate::entity::{
    order_items::{ActiveModel as ActiveOrderItem, Entity as OrderItemEntity},
    orders::{ActiveModel as ActiveOrder, Entity as Order},
    plants::{Column as PlantColumn, Entity as Plant, Model as PlantModel},
};
use crate::errors::AppError;
use crate::orders::models::OrderSummary;
use crate::orders::query::{summaries_for_user, summary_by_id};

// ==============================================================================
// Structures
// ==============================================================================

/// DTO pour un item dans une nouvelle commande.
#[derive(Deserialize, Clone)]
pub struct NewOrderItemDto {
    #[serde(alias = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
}

/// Payload pour la creation d'une commande.
#[derive(Deserialize)]
pub struct NewOrderPayload {
    pub items: Vec<NewOrderItemDto>,
}

/// DTO pour la mise a jour du statut d'une commande.
#[derive(Deserialize)]
pub struct UpdateOrderDto {
    pub status: Option<String>,
}

// ==============================================================================
// Handlers
// ==============================================================================

/// Création d'une commande utilisateur courant (JWT obligatoire)
#[handler]
pub async fn create_order(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
    Json(payload): Json<NewOrderPayload>,
) -> PoemResult<(StatusCode, Json<OrderSummary>)> {
    let user_id = auth.user_id();
    if payload.items.is_empty() {
        return Err(AppError::Conflict.into());
    }
    let txn = db.begin().await.map_err(|_| AppError::Internal)?;

    let mut total: i32 = 0;
    let plant_ids: Vec<i32> = payload.items.iter().map(|i| i.plant_id).collect();
    let plants = Plant::find()
        .filter(PlantColumn::Id.is_in(plant_ids.clone()))
        .all(&txn)
        .await
        .map_err(|_| AppError::Internal)?;
    let mut plant_map: HashMap<i32, PlantModel> = HashMap::new();
    for plant in plants {
        plant_map.insert(plant.id, plant);
    }

    let new_order = ActiveOrder {
        user_id: Set(Some(user_id)),
        total: Set(total),
        ..Default::default()
    };
    let inserted_order = new_order
        .insert(&txn)
        .await
        .map_err(|_| AppError::Internal)?;
    let order_id = inserted_order.id;

    let mut order_items = Vec::with_capacity(payload.items.len());
    for item in &payload.items {
        let plant = plant_map.get(&item.plant_id).ok_or(AppError::NotFound)?;
        if plant.stock < item.quantity {
            txn.rollback().await.map_err(|_| AppError::Internal)?;
            return Err(AppError::Conflict.into());
        }
        let price = plant.price;
        total += price * item.quantity;
        order_items.push(ActiveOrderItem {
            order_id: Set(Some(inserted_order.id)),
            plant_id: Set(Some(plant.id)),
            quantity: Set(item.quantity),
            price: Set(price),
            ..Default::default()
        });
    }

    if !order_items.is_empty() {
        OrderItemEntity::insert_many(order_items)
            .exec(&txn)
            .await
            .map_err(|_| AppError::Internal)?;
    }

    let mut updated_order = inserted_order.into_active_model();
    updated_order.total = Set(total);
    updated_order
        .update(&txn)
        .await
        .map_err(|_| AppError::Internal)?;
    txn.commit().await.map_err(|_| AppError::Internal)?;

    let summary = summary_by_id(db, order_id).await?;
    Ok((StatusCode::CREATED, Json(summary)))
}

/// Liste des commandes de l’utilisateur courant
#[handler]
pub async fn list_orders(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
) -> Result<Json<Vec<OrderSummary>>, AppError> {
    let user_id = auth.user_id();
    let summaries = summaries_for_user(db, user_id).await?;
    Ok(Json(summaries))
}

/// Lecture d’une commande complète (avec items)
#[handler]
pub async fn get_order(
    Data(db): Data<&DatabaseConnection>,
    Path(order_id): Path<i32>,
) -> PoemResult<Json<OrderSummary>> {
    let summary = summary_by_id(db, order_id).await?;
    Ok(Json(summary))
}

/// Mise a jour du statut de commande.
///
/// @param db Connection a la base de donnees
/// @param order_id ID de la commande
/// @param payload Nouveau statut
/// @return Json<OrderSummary> mis a jour ou erreur
#[handler]
pub async fn update_order(
    Data(db): Data<&DatabaseConnection>,
    Path(order_id): Path<i32>,
    Json(payload): Json<UpdateOrderDto>,
) -> PoemResult<Json<OrderSummary>> {
    let existing = Order::find_by_id(order_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    let mut active = existing.into_active_model();
    if let Some(status) = payload.status.clone() {
        active.status = Set(status);
    }

    active.update(db).await.map_err(|_| AppError::Internal)?;
    let summary = summary_by_id(db, order_id).await?;
    Ok(Json(summary))
}

/// Suppression d’une commande
#[handler]
pub async fn delete_order(
    Data(db): Data<&DatabaseConnection>,
    Path(order_id): Path<i32>,
) -> PoemResult<()> {
    Order::delete_by_id(order_id)
        .exec(db)
        .await
        .map_err(|_| AppError::Internal)?;
    Ok(())
}
