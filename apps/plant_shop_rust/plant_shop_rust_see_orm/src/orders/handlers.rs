/// Gestion des commandes avec SeaORM
use poem::{
	handler,
	web::{Data, Json, Path},
	web::cookie::CookieJar,
	http::StatusCode,
	Result as PoemResult,
};
use serde::Deserialize;
use sea_orm::{
	DatabaseConnection, Set, ActiveModelTrait, EntityTrait, QueryFilter, ColumnTrait,
	TransactionTrait, QueryOrder, IntoActiveModel, QuerySelect,
};

use crate::errors::AppError;
use crate::auth::jwt::verify_jwt;
use crate::entity::{
	orders::{Entity as Order, Model as OrderModel, ActiveModel as ActiveOrder, Column as OrderColumn},
	order_items::{ActiveModel as ActiveOrderItem},
	plants::{Entity as Plant},
};
use crate::entity::order_items;
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
	jar: &CookieJar,
	Json(payload): Json<NewOrderPayload>,
) -> PoemResult<(StatusCode, Json<OrderModel>)> {
	let user_id = if let Some(c) = jar.get("auth_token") {
		let token = c.value_str();
		let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
		let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
		claims.sub
	} else {
		return Err(AppError::Unauthorized.into());
	};

	let txn = db.begin().await.map_err(|_| AppError::Internal)?;
	let mut total: i32 = 0;

	let new_order = ActiveOrder {
		user_id: Set(Some(user_id)),
		total: Set(total),
		..Default::default()
	};
	let inserted_order = new_order.insert(&txn).await.map_err(|_| AppError::Internal)?;

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
		new_item.insert(&txn).await.map_err(|_| AppError::Internal)?;
	}

	let mut updated_order = inserted_order.clone().into_active_model();
	updated_order.total = Set(total);
	updated_order.update(&txn).await.map_err(|_| AppError::Internal)?;
	txn.commit().await.map_err(|_| AppError::Internal)?;

	Ok((StatusCode::CREATED, Json(inserted_order)))
}

/// Liste des commandes de l’utilisateur courant
#[handler]
pub async fn list_orders(
    Data(db): Data<&DatabaseConnection>,
    jar: &CookieJar,
) -> Result<Json<Vec<serde_json::Value>>, AppError> {
    // Auth
    let user_id = if let Some(c) = jar.get("auth_token") {
        let token = c.value_str();
        let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
        let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
        claims.sub
    } else {
        return Err(AppError::Unauthorized);
    };

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
            // Cherche le nom de la plante si possible
            let plant_name = if let Some(pid) = item.plant_id {
                match Plant::find_by_id(pid).one(db).await {
                    Ok(Some(plant)) => plant.name,
                    _ => String::new(),
                }
            } else {
                String::new()
            };

						item_details.push(json!({
								"id": item.id,
								"plantId": item.plant_id,
								"quantity": item.quantity,
								"price": item.price,
								"plant": {
										"id": item.plant_id,
										"name": plant_name
								}
						}));
        }

        response.push(json!({
            "id": order.id,
            "status": order.status,
            "total": order.total,
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
pub async fn delete_order(Data(db): Data<&DatabaseConnection>, Path(order_id): Path<i32>) -> PoemResult<()> {
	Order::delete_by_id(order_id)
		.exec(db)
		.await
		.map_err(|_| AppError::Internal)?;
	Ok(())
}
