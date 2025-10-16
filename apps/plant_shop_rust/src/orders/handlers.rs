/// Handlers Poem pour gestion des commandes
use poem::{handler, web::{Data, Json, Path}, Result as PoemResult, IntoResponse};
use serde::Deserialize; // Import manquant
use sqlx::PgPool;
use uuid::Uuid;
use crate::errors::AppError;
use super::models::{Order, UpdateOrder, OrderItemPayload, OrderWithItems};

// Structure pour le payload de création de commande
#[derive(Deserialize)]
pub struct NewOrderPayload {
    pub items: Vec<OrderItemPayload>,
}

#[handler]
pub async fn create_order(
	Data(pool): Data<&PgPool>,
    // TODO: Récupérer user_id depuis le token JWT
	Json(payload): Json<NewOrderPayload>,
) -> PoemResult<Json<OrderWithItems>> {
    let user_id = Uuid::new_v4(); // Temporaire: à remplacer par l'ID de l'utilisateur authentifié
    let mut tx = pool.begin().await.map_err(|e| AppError::DatabaseError(e))?
;
    let mut total = sqlx::types::BigDecimal::from(0);

    // Créer la commande avec un total de 0 pour l'instant
    let order = sqlx::query_as!(
        Order,
        "INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING id, user_id, total, status, created_at",
        user_id,
        total
    )
    .fetch_one(&mut *tx)
    .await.map_err(|e| AppError::DatabaseError(e))?
;

    let mut created_items = Vec::new();

    for item in payload.items {
        let plant = sqlx::query!("SELECT price, stock FROM plants WHERE id = $1", item.plant_id)
            .fetch_optional(&mut *tx)
            .await.map_err(|e| AppError::DatabaseError(e))?

            .ok_or(AppError::NotFound)?;

        if plant.stock < item.quantity as i32 {
            tx.rollback().await.map_err(|e| AppError::DatabaseError(e))?
;
            return Err(AppError::Conflict.into());
        }

        let item_price = plant.price.clone();
        let item_total = item_price.clone() * sqlx::types::BigDecimal::from(item.quantity);
        total += item_total;

        let order_item = sqlx::query_as!(
            crate::order_items::models::OrderItem,
            "INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES ($1, $2, $3, $4) RETURNING *",
            order.id, item.plant_id, item.quantity as i32, item_price
        ).fetch_one(&mut *tx).await.map_err(|e| AppError::DatabaseError(e))?
;
        created_items.push(order_item);
    }

    // Mettre à jour le total final de la commande
    sqlx::query!(
        "UPDATE orders SET total = $1 WHERE id = $2",
        total,
        order.id
    ).execute(&mut *tx).await.map_err(|e| AppError::DatabaseError(e))?
;

    tx.commit().await.map_err(|e| AppError::DatabaseError(e))?
;

    let response = OrderWithItems {
        id: order.id,
        user_id: order.user_id,
        total,
        status: order.status,
        created_at: order.created_at,
        items: created_items,
    };

	Ok(Json(response))
}

#[handler]
pub async fn list_orders(
	Data(pool): Data<&PgPool>
) -> PoemResult<Json<Vec<Order>>> {
	let orders = sqlx::query_as!(
		Order,
		"SELECT id, user_id, total, status, created_at FROM orders"
	)
	.fetch_all(pool)
	.await.map_err(|e| AppError::DatabaseError(e))?
;
	Ok(Json(orders))
}

#[handler]
pub async fn get_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
) -> PoemResult<Json<Order>> {
	let order = sqlx::query_as!(
		Order,
		"SELECT id, user_id, total, status, created_at FROM orders WHERE id = $1",
		order_id
	)
	.fetch_one(pool)
	.await.map_err(|e| AppError::DatabaseError(e))?
;
	Ok(Json(order))
}

#[handler]
pub async fn update_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
	Json(payload): Json<UpdateOrder>,
) -> PoemResult<Json<Order>> {
	let order = sqlx::query_as!(
		Order,
		"UPDATE orders SET
			status = COALESCE($1, status)
		 WHERE id = $2
		 RETURNING id, user_id, total, status, created_at",
		payload.status,
		order_id
	)
	.fetch_one(pool)
	.await.map_err(|e| AppError::DatabaseError(e))?
;
	Ok(Json(order))
}

#[handler]
pub async fn delete_order(
	Data(pool): Data<&PgPool>,
	Path(order_id): Path<Uuid>,
) -> PoemResult<()> {
	let result = sqlx::query!("DELETE FROM orders WHERE id = $1", order_id)
		.execute(pool)
		.await.map_err(|e| AppError::DatabaseError(e))?
;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound.into());
    }

	Ok(())
}
