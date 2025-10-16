/// Handlers Poem pour gestion des commandes
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult};
use sqlx::PgPool;
use uuid::Uuid;
use crate::errors::AppError;
use super::models::{Order, NewOrder};

#[handler]
pub async fn create_order(
	Data(pool): Data<&PgPool>,
	Json(payload): Json<NewOrder>,
) -> PoemResult<Json<Order>, AppError> {
	let order = sqlx::query_as!(
		Order,
		"INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING *",
		payload.user_id,
		payload.total
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::Conflict)?;
	Ok(Json(order))
}

#[handler]
pub async fn list_orders(
	Data(pool): Data<&PgPool>
) -> PoemResult<Json<Vec<Order>>, AppError> {
	let orders = sqlx::query_as!(
		Order,
		"SELECT * FROM orders"
	)
	.fetch_all(pool)
	.await
	.map_err(|_| AppError::Internal)?;
	Ok(Json(orders))
}

#[handler]
pub async fn get_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
) -> PoemResult<Json<Order>, AppError> {
	let order = sqlx::query_as!(
		Order,
		"SELECT * FROM orders WHERE id = $1",
		order_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(order))
}

#[handler]
pub async fn update_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
	Json(payload): Json<NewOrder>,
) -> PoemResult<Json<Order>, AppError> {
	let order = sqlx::query_as!(
		Order,
		"UPDATE orders SET
			user_id = $1,
			total = $2
		 WHERE id = $3
		 RETURNING *",
		payload.user_id,
		payload.total,
		order_id
	)
	.fetch_one(pool)
	.await
	.map_err(|_| AppError::NotFound)?;
	Ok(Json(order))
}

#[handler]
pub async fn delete_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
) -> PoemResult<(), AppError> {
	sqlx::query!("DELETE FROM orders WHERE id = $1", (order_id,))
		.execute(pool)
		.await
		.map_err(|_| AppError::NotFound)?;
	Ok(())
}
