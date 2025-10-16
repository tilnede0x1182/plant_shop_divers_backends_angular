/// Handlers Poem pour gestion des éléments de commande
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sqlx::PgPool;
use uuid::Uuid;
use crate::errors::AppError;
use super::models::{OrderItem, NewOrderItem};

#[handler]
pub async fn get_order_item(
	Data(pool): Data<&PgPool>,
	Path(order_item_id): Path<Uuid>,
) -> PoemResult<Json<OrderItem>, AppError> {
	let item = sqlx::query_as!(
		OrderItem,
		"SELECT * FROM order_items WHERE id = $1",
		order_item_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(item))
}

#[handler]
pub async fn update_order_item(
	Data(pool): Data<&PgPool>,
	Path(order_item_id): Path<Uuid>,
	Json(payload): Json<NewOrderItem>,
) -> PoemResult<Json<OrderItem>, AppError> {
	let item = sqlx::query_as!(
		OrderItem,
		"UPDATE order_items SET
			order_id = $1,
			plant_id = $2,
			quantity = $3,
			price = $4
		 WHERE id = $5
		 RETURNING *",
		payload.order_id,
		payload.plant_id,
		payload.quantity,
		payload.price,
		order_item_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(item))
}

#[handler]
pub async fn delete_order_item(
	Data(pool): Data<&PgPool>,
	Path(order_item_id): Path<Uuid>,
) -> PoemResult<(), AppError> {
	sqlx::query!("DELETE FROM order_items WHERE id = $1", (order_item_id,))
		.execute(pool)
		.await
		.map_err(|_| AppError::NotFound)?;
	Ok(())
}
