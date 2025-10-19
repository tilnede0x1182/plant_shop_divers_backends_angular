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
	DatabaseConnection, Set, ActiveModelTrait, EntityTrait, QueryFilter, ColumnTrait, TransactionTrait,
};
use crate::errors::AppError;
use crate::auth::jwt::verify_jwt;
use crate::entity::{
	orders::{self, Entity as Order, Model as OrderModel, ActiveModel as ActiveOrder, Column as OrderColumn},
	order_items::{self, Entity as OrderItem, ActiveModel as ActiveOrderItem, Model as OrderItemModel},
	plants::{self, Entity as Plant, Column as PlantColumn, Model as PlantModel},
};
use sea_orm::prelude::*;
use bigdecimal::BigDecimal;
use std::str::FromStr;

// Structure pour le payload de création de commande
#[derive(Deserialize)]
pub struct NewOrderPayload {
	pub items: Vec<OrderItemModel>,
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
	let mut total = BigDecimal::from(0);

	let new_order = ActiveOrder {
		user_id: Set(Some(user_id)),
		total: Set(total.clone()),
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

		let item_price = plant.price.clone();
		let q = BigDecimal::from(item.quantity);
		total += item_price.clone() * q;

		let new_item = ActiveOrderItem {
			order_id: Set(inserted_order.id),
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
) -> Result<Json<Vec<OrderModel>>, AppError> {
	let user_id = if let Some(c) = jar.get("auth_token") {
		let token = c.value_str();
		let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
		let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
		claims.sub
	} else {
		return Err(AppError::Unauthorized);
	};

	let orders = Order::find()
		.filter(OrderColumn::UserId.eq(Some(user_id)))
		.all(db)
		.await
		.map_err(|_| AppError::Internal)?;

	Ok(Json(orders))
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
	Json(payload): Json<orders::Model>,
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
