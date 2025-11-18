/// Gestion des commandes avec SeaORM
use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Result as PoemResult,
};
use sea_orm::{
    ActiveModelTrait, DatabaseConnection, EntityTrait, IntoActiveModel, QueryOrder, Set,
    TransactionTrait,
};
use serde::Deserialize;

use crate::auth::session::AuthSession;
use crate::entity::{
    order_items::ActiveModel as ActiveOrderItem,
    orders::{
        ActiveModel as ActiveOrder, Column as OrderColumn, Entity as Order, Model as OrderModel,
    },
    plants::Entity as Plant,
};
use crate::errors::AppError;
use serde_json::json;

/// DTO pour création de commande (contient id plante + quantité)
#[derive(Deserialize, Clone)]
pub struct NewOrderItemDto {
    #[serde(alias = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
}

/// Payload pour la création d'une commande
#[derive(Deserialize)]
pub struct NewOrderPayload {
    pub items: Vec<NewOrderItemDto>,
}

/// Mise à jour du statut de commande
#[derive(Deserialize)]
pub struct UpdateOrderDto {
    pub status: Option<String>,
}

/// Création d’une commande utilisateur courant (JWT obligatoire)
#[handler]
pub async fn create_order(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
    Json(payload): Json<NewOrderPayload>,
) -> PoemResult<(StatusCode, Json<OrderModel>)> {
    let user_id = auth.user_id();

    let txn = db.begin().await.map_err(|_| AppError::Internal)?;
    let mut total: i32 = 0;

    let new_order = ActiveOrder {
        user_id: Set(Some(user_id)),
        total: Set(total),
        ..Default::default()
    };
    let inserted_order = new_order
        .insert(&txn)
        .await
        .map_err(|_| AppError::Internal)?;

    for item in &payload.items {
        let plant = Plant::find_by_id(item.plant_id)
            .one(&txn)
            .await
            .map_err(|_| AppError::Internal)?
            .ok_or(AppError::NotFound)?;

        if plant.stock < item.quantity {
            txn.rollback().await.map_err(|_| AppError::Internal)?;
            return Err(AppError::Conflict.into());
        }

        let item_price = plant.price;
        total += item_price * item.quantity;

        let new_item = ActiveOrderItem {
            order_id: Set(Some(inserted_order.id)),
            plant_id: Set(Some(plant.id)),
            quantity: Set(item.quantity),
            price: Set(item_price),
            ..Default::default()
        };
        new_item
            .insert(&txn)
            .await
            .map_err(|_| AppError::Internal)?;
    }

    let mut updated_order = inserted_order.clone().into_active_model();
    updated_order.total = Set(total);
    updated_order
        .update(&txn)
        .await
        .map_err(|_| AppError::Internal)?;
    txn.commit().await.map_err(|_| AppError::Internal)?;

    Ok((StatusCode::CREATED, Json(inserted_order)))
}

/// Liste des commandes de l’utilisateur courant
#[handler]
pub async fn list_orders(
    Data(db): Data<&DatabaseConnection>,
    auth: AuthSession,
) -> Result<Json<Vec<serde_json::Value>>, AppError> {
    // Auth
    let user_id = auth.user_id();

    use crate::entity::order_items;
    use sea_orm::prelude::*;
    use serde_json::json;

    // Récupération commandes + items
    let orders_with_items: Vec<(OrderModel, Vec<order_items::Model>)> = Order::find()
        .filter(OrderColumn::UserId.eq(Some(user_id)))
        .order_by_desc(OrderColumn::CreatedAt)
        .find_with_related(order_items::Entity)
        .all(db)
        .await
        .map_err(|_| AppError::Internal)?;

    // Mapping JSON attendu par les tests
    let mut response = Vec::new();

    for (order, items) in orders_with_items {
        let mut item_details = Vec::new();

        for item in items {
            let pid = match item.plant_id {
                Some(pid) => pid,
                None => continue,
            };

            // Ignore l'item si la plante n'existe plus
            let plant = match Plant::find_by_id(pid).one(db).await {
                Ok(Some(plant)) => plant,
                _ => continue,
            };

            item_details.push(json!({
                "id": item.id,
                "plantId": pid,
                "quantity": item.quantity,
                "price": item.price,
                "plant": {
                    "id": plant.id,
                    "name": plant.name,
                    "price": plant.price,
                }
            }));
        }

        response.push(json!({
            "id": order.id,
            "status": order.status,
            "totalPrice": order.total,
            "createdAt": order.created_at,
            "orderItems": item_details
        }));
    }

    Ok(Json(response))
}

/// Lecture d’une commande complète (avec items)
#[handler]
pub async fn get_order(
    Data(db): Data<&DatabaseConnection>,
    Path(order_id): Path<i32>,
) -> PoemResult<Json<OrderModel>> {
    let order = Order::find_by_id(order_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    Ok(Json(order))
}

/// Mise à jour du statut de commande
#[handler]
pub async fn update_order(
    Data(db): Data<&DatabaseConnection>,
    Path(order_id): Path<i32>,
    Json(payload): Json<UpdateOrderDto>,
) -> PoemResult<Json<OrderModel>> {
    let existing = Order::find_by_id(order_id)
        .one(db)
        .await
        .map_err(|_| AppError::Internal)?
        .ok_or(AppError::NotFound)?;

    let mut active = existing.into_active_model();
    if let Some(status) = payload.status.clone() {
        active.status = Set(status);
    }

    let updated = active.update(db).await.map_err(|_| AppError::Internal)?;
    Ok(Json(updated))
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
