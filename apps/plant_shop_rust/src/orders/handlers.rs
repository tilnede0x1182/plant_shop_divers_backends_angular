// Gestion des commandes (handlers Poem)
use poem::{
	handler,
	web::{Data, Json, Path},
	web::cookie::CookieJar,
	http::StatusCode,
	Result as PoemResult,
};
use serde::Deserialize;
use sqlx::PgPool;

use crate::errors::AppError;
use crate::auth::jwt::verify_jwt;
use super::models::{
	Order,
	UpdateOrder,
	OrderItemPayload,
	OrderWithItems,
	OrderItemWithPlant,
	PlantBasic,
};

// Structure pour le payload de création de commande
#[derive(Deserialize)]
pub struct NewOrderPayload {
    pub items: Vec<OrderItemPayload>,
}

/// Création de commande utilisateur courant (@jar cookie JWT → user_id, 201 en sortie)
#[handler]
pub async fn create_order(
    Data(pool): Data<&PgPool>,
    jar: &CookieJar,
    Json(payload): Json<NewOrderPayload>,
) -> PoemResult<(StatusCode, Json<OrderWithItems>)> {
    // tentative d’extraction du user_id depuis le cookie
    let user_id = if let Some(c) = jar.get("auth_token") {
        let token = c.value_str();
        let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
        let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
        claims.sub
    } else {
        // fallback : dernier utilisateur non-admin créé
        let row = sqlx::query!("SELECT id FROM users WHERE is_admin = false ORDER BY created_at DESC LIMIT 1")
            .fetch_one(pool)
            .await
            .map_err(|e| AppError::DatabaseError(e))?;
        row.id
    };

    let mut tx = pool.begin().await.map_err(|e| AppError::DatabaseError(e))?;
    let mut total = sqlx::types::BigDecimal::from(0);

	let order = sqlx::query_as!(
		Order,
		"INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING id,user_id, total, status, created_at",
		user_id,
		total
	)
	.fetch_one(&mut *tx)
	.await.map_err(|e| AppError::DatabaseError(e))?;

	let mut created_items = Vec::new();
	for item in payload.items {
		let plant = sqlx::query!("SELECT price, stock FROM plants WHERE id = $1", item.plant_id)
			.fetch_optional(&mut *tx).await.map_err(|e| AppError::DatabaseError(e))?
			.ok_or(AppError::NotFound)?;
		if plant.stock < item.quantity as i32 {
			tx.rollback().await.map_err(|e| AppError::DatabaseError(e))?;
			return Err(AppError::Conflict.into());
		}
		let item_price = plant.price.clone();
		let item_total = item_price.clone() * sqlx::types::BigDecimal::from(item.quantity);
		total += item_total;
		let order_item = sqlx::query_as!(
			crate::order_items::models::OrderItem,
			"INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES ($1, $2, $3, $4) RETURNING id, order_id, plant_id, quantity, price",
			order.id, item.plant_id, item.quantity as i32, item_price
		)
		.fetch_one(&mut *tx).await.map_err(|e| AppError::DatabaseError(e))?;
		created_items.push(order_item);
	}

	sqlx::query!("UPDATE orders SET total = $1 WHERE id = $2", total, order.id)
		.execute(&mut *tx).await.map_err(|e| AppError::DatabaseError(e))?;
	tx.commit().await.map_err(|e| AppError::DatabaseError(e))?;

	let response = OrderWithItems {
			id: order.id,
			user_id: order.user_id,
			total,
			status: order.status,
			created_at: order.created_at,
			items: {
				let mut items_vec = Vec::new();
				for order_item in &created_items {
					let plant = sqlx::query_as!(
						PlantBasic,
						"SELECT id,name, price, stock, description FROM plants WHERE id = $1",
						order_item.plant_id
					)
					.fetch_one(pool)
					.await
					.map_err(|e| AppError::DatabaseError(e))?;
					items_vec.push(OrderItemWithPlant {
						id: order_item.id,
						quantity: order_item.quantity,
						price: order_item.price.clone(),
						plant,
					});
				}
				items_vec
			},
	};
	Ok((StatusCode::CREATED, Json(response)))
}

#[handler]
pub async fn list_orders(
	Data(pool): Data<&PgPool>,
	jar: &CookieJar,
) -> Result<Json<Vec<OrderWithItems>>, AppError> {
	let token = jar
		.get("auth_token")
		.map(|c| c.value_str().to_string())
		.ok_or(AppError::Unauthorized)?;
	let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
	let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;
	let user_id = claims.sub;

	// Requête principale : commandes du user
	let orders = sqlx::query!(
		"SELECT id,user_id, total, status, created_at FROM orders WHERE user_id = $1",
		user_id
	)
	.fetch_all(pool)
	.await
	.map_err(AppError::DatabaseError)?;

	let mut results = Vec::new();

	for order in orders {
		let items = sqlx::query!(
			"SELECT oi.id, oi.quantity, oi.price, p.id as plant_id, p.name, p.price as plant_price, p.stock, p.description
			 FROM order_items oi
			 JOIN plants p ON oi.plant_id = p.id
			 WHERE oi.order_id = $1",
			order.id
		)
		.fetch_all(pool)
		.await
		.map_err(AppError::DatabaseError)?
		.into_iter()
		.map(|row| OrderItemWithPlant {
			id: row.id,
			quantity: row.quantity,
			price: row.price,
			plant: PlantBasic {
				id: row.plant_id,
				name: row.name,
				price: row.plant_price,
				stock: row.stock,
				description: row.description,
			},
		})
		.collect();

		results.push(OrderWithItems {
				id: order.id,
				user_id: order.user_id,
				total: order.total,
				status: order.status,
				created_at: order.created_at,
				items,
		});
	}
	Ok(Json(results))
}

#[handler]
pub async fn get_order(
    Data(pool): Data<&PgPool>,
    Path(order_id): Path<i32>,
) -> PoemResult<Json<OrderWithItems>> {
	let order = sqlx::query_as!(
		Order,
		"SELECT id,user_id, total, status, created_at FROM orders WHERE id = $1",
		order_id
	)
	.fetch_one(pool)
	.await
	.map_err(|e| AppError::DatabaseError(e))?;

	let items = sqlx::query!(
		"SELECT oi.id, oi.quantity, oi.price, p.id as plant_id, p.name, p.price as plant_price, p.stock, p.description
		 FROM order_items oi
		 JOIN plants p ON oi.plant_id = p.id
		 WHERE oi.order_id = $1",
		order.id
	)
	.fetch_all(pool)
	.await
	.map_err(AppError::DatabaseError)?
	.into_iter()
	.map(|row| OrderItemWithPlant {
		id: row.id,
		quantity: row.quantity,
		price: row.price,
		plant: PlantBasic {
			id: row.plant_id,
			name: row.name,
			price: row.plant_price,
			stock: row.stock,
			description: row.description,
		},
	})
	.collect();

	let order_with_items = OrderWithItems {
		id: order.id,
		user_id: order.user_id,
		total: order.total,
		status: order.status,
		created_at: order.created_at,
		items,
	};
	Ok(Json(order_with_items))
}


#[handler]
pub async fn update_order(
    Data(pool): Data<&PgPool>,
    Path(order_id): Path<i32>,
    Json(payload): Json<UpdateOrder>,
) -> PoemResult<Json<Order>> {
	let order = sqlx::query_as!(
		Order,
		"UPDATE orders SET
			status = COALESCE($1, status)
		 WHERE id = $2
		 RETURNING id,user_id, total, status, created_at",
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
    Path(order_id): Path<i32>,
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
