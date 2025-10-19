/// Handlers Poem pour gestion des éléments de commande (SeaORM)
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sea_orm::{DatabaseConnection, EntityTrait, ActiveModelTrait, Set, IntoActiveModel};
use crate::errors::AppError;
use crate::entity::order_items::{Entity as OrderItem, Model as OrderItemModel, ActiveModel as ActiveOrderItem};

#[handler]
pub async fn get_order_item(
	Data(db): Data<&DatabaseConnection>,
	Path(order_item_id): Path<i32>,
) -> PoemResult<Json<OrderItemModel>, AppError> {
	let item = OrderItem::find_by_id(order_item_id)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::NotFound)?;
	Ok(Json(item))
}

#[handler]
pub async fn update_order_item(
	Data(db): Data<&DatabaseConnection>,
	Path(order_item_id): Path<i32>,
	Json(payload): Json<OrderItemModel>,
) -> PoemResult<Json<OrderItemModel>, AppError> {
	let existing = OrderItem::find_by_id(order_item_id)
		.one(db)
		.await
		.map_err(|_| AppError::Internal)?
		.ok_or(AppError::NotFound)?;

	let mut active = existing.into_active_model();
	active.order_id = Set(payload.order_id);
	active.plant_id = Set(payload.plant_id);
	active.quantity = Set(payload.quantity);
	active.price = Set(payload.price);

	let updated = active.update(db).await.map_err(|_| AppError::Internal)?;
	Ok(Json(updated))
}

#[handler]
pub async fn delete_order_item(Data(db): Data<&DatabaseConnection>, Path(order_item_id): Path<i32>) -> PoemResult<(), AppError> {
	OrderItem::delete_by_id(order_item_id)
		.exec(db)
		.await
		.map_err(|_| AppError::Internal)?;
	Ok(())
}
