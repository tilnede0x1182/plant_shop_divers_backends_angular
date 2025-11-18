use super::models::{NewOrderItem, OrderItem};
use crate::errors::AppError;
use crate::state::AppState;
/// Handlers Poem pour gestion des éléments de commande
use poem::{
    handler,
    web::{Data, Json, Path},
    Result as PoemResult,
};

#[handler]
pub async fn get_order_item(
    Data(state): Data<&AppState>,
    Path(order_item_id): Path<i32>,
) -> PoemResult<Json<OrderItem>, AppError> {
    let item = sqlx::query_as!(
        OrderItem,
        "SELECT id, order_id, plant_id, quantity, price
 		FROM order_items WHERE id = $1",
        order_item_id
    )
    .fetch_one(state.read_pool())
    .await
    .map_err(|_| AppError::NotFound)?;
    Ok(Json(item))
}

#[handler]
pub async fn update_order_item(
    Data(state): Data<&AppState>,
    Path(order_item_id): Path<i32>,
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
		 RETURNING id, order_id, plant_id, quantity, price",
        payload.order_id,
        payload.plant_id,
        payload.quantity,
        payload.price,
        order_item_id
    )
    .fetch_one(state.write_pool())
    .await
    .map_err(|_| AppError::NotFound)?;
    Ok(Json(item))
}

#[handler]
pub async fn delete_order_item(
    Data(state): Data<&AppState>,
    Path(order_item_id): Path<i32>,
) -> PoemResult<(), AppError> {
    sqlx::query!("DELETE FROM order_items WHERE id = $1", order_item_id)
        .execute(state.write_pool())
        .await
        .map_err(|_| AppError::NotFound)?;
    Ok(())
}
