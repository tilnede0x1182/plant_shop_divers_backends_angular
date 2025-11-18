use std::collections::HashMap;

use poem::{
    handler,
    http::StatusCode,
    web::{Data, Json, Path},
    Response, Result as PoemResult,
};
use serde::Deserialize;
use sqlx::{types::BigDecimal, PgPool, Postgres, QueryBuilder};

use crate::auth::session::AuthSession;
use crate::dto::{OrderItemPlant, OrderItemResponse, OrderSummary};
use crate::errors::AppError;
use crate::logging::log_debug_lazy;
use crate::plants::models::PriceExt;
use crate::response::buffered_json;
use crate::state::AppState;

#[derive(Deserialize, Clone)]
pub struct NewOrderItemDto {
    #[serde(alias = "plantId")]
    pub plant_id: i32,
    pub quantity: i32,
}

#[derive(Deserialize)]
pub struct NewOrderPayload {
    pub items: Vec<NewOrderItemDto>,
}

#[derive(Deserialize)]
pub struct UpdateOrderDto {
    pub status: Option<String>,
}

#[handler]
pub async fn create_order(
    Data(state): Data<&AppState>,
    auth: AuthSession,
    Json(payload): Json<NewOrderPayload>,
) -> PoemResult<(StatusCode, Json<OrderSummary>)> {
    let mut tx = state
        .write_pool()
        .begin()
        .await
        .map_err(|e| AppError::DatabaseError(e))?;

    let plant_ids: Vec<i32> = payload.items.iter().map(|item| item.plant_id).collect();

    log_debug_lazy(|| {
        format!(
            "[orders] create_order for user {} with {} items",
            auth.user_id(),
            payload.items.len()
        )
    });

    let plant_rows = sqlx::query!(
        "SELECT id, name, price, stock FROM plants WHERE id = ANY($1)",
        &plant_ids
    )
    .fetch_all(&mut *tx)
    .await
    .map_err(|e| {
        log_debug_lazy(|| format!("[orders] plant lookup failed: {e}"));
        AppError::DatabaseError(e)
    })?;

    let mut plants = HashMap::new();
    for row in plant_rows {
        plants.insert(row.id, row);
    }

    let mut total = BigDecimal::from(0);
    let mut inserts = Vec::new();

    let order = sqlx::query!(
        "INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING id, user_id, total, status, created_at",
        auth.user_id(),
        total
    )
    .fetch_one(&mut *tx)
    .await
    .map_err(|e| {
        log_debug_lazy(|| format!("[orders] failed inserting order header: {e}"));
        AppError::DatabaseError(e)
    })?;

    for item in &payload.items {
        let plant = plants.get(&item.plant_id).ok_or(AppError::NotFound)?;
        if plant.stock < item.quantity {
            tx.rollback().await.ok();
            return Err(AppError::Conflict.into());
        }
        let price = plant.price.clone();
        total += price.clone() * BigDecimal::from(item.quantity);
        inserts.push((item.plant_id, item.quantity, price));
    }

    let inserted_items = if !inserts.is_empty() {
        let mut builder = QueryBuilder::<Postgres>::new(
            "INSERT INTO order_items (order_id, plant_id, quantity, price) ",
        );
        builder.push_values(inserts.iter(), |mut b, (plant_id, quantity, price)| {
            b.push_bind(order.id)
                .push_bind(*plant_id)
                .push_bind(*quantity)
                .push_bind(price.clone());
        });
        builder.push(" RETURNING id, plant_id, quantity, price");

        log_debug_lazy(|| format!("[orders] bulk insert SQL: {}", builder.sql()));

        builder
            .build_query_as::<InsertedOrderItemRow>()
            .fetch_all(&mut *tx)
            .await
            .map_err(|e| {
                log_debug_lazy(|| format!("[orders] bulk insert order_items failed: {e}"));
                AppError::DatabaseError(e)
            })?
    } else {
        Vec::new()
    };

    sqlx::query!(
        "UPDATE orders SET total = $1 WHERE id = $2",
        total,
        order.id
    )
    .execute(&mut *tx)
    .await
    .map_err(|e| {
        log_debug_lazy(|| format!("[orders] failed updating total for order {}: {e}", order.id));
        AppError::DatabaseError(e)
    })?;

    tx.commit().await.map_err(|e| AppError::DatabaseError(e))?;

    let mut summary_items = Vec::with_capacity(inserted_items.len());
    for item in inserted_items {
        if let Some(plant) = plants.get(&item.plant_id) {
            let plant_view = OrderItemPlant {
                id: plant.id,
                name: plant.name.clone(),
                price: plant.price.as_i32_lossy(),
            };
            summary_items.push(OrderItemResponse::new(
                item.id,
                item.plant_id,
                item.quantity,
                item.price.as_i32_lossy(),
                plant_view,
            ));
        }
    }

    let summary = OrderSummary::new(
        order.id,
        order.status.clone(),
        total.as_i32_lossy(),
        order.created_at,
        summary_items,
    );

    Ok((StatusCode::CREATED, Json(summary)))
}

#[handler]
pub async fn list_orders(
    Data(state): Data<&AppState>,
    auth: AuthSession,
) -> Result<Response, AppError> {
    log_debug_lazy(|| format!("[orders] list_orders start for user {}", auth.user_id()));
    let rows = fetch_rows_for_user(state.read_pool(), auth.user_id()).await?;
    log_debug_lazy(|| format!("[orders] list_orders fetched {} db rows", rows.len()));
    let summaries = fold_rows(rows);
    log_debug_lazy(|| format!("[orders] list_orders returning {} orders", summaries.len()));
    buffered_json(&summaries, StatusCode::OK)
}

#[handler]
pub async fn get_order(
    Data(state): Data<&AppState>,
    Path(order_id): Path<i32>,
) -> Result<Response, AppError> {
    let summary = fetch_single_summary(state.read_pool(), order_id).await?;
    buffered_json(&summary, StatusCode::OK)
}

#[handler]
pub async fn update_order(
    Data(state): Data<&AppState>,
    Path(order_id): Path<i32>,
    Json(payload): Json<UpdateOrderDto>,
) -> PoemResult<Json<OrderSummary>> {
    sqlx::query!(
        "UPDATE orders SET status = COALESCE($1, status) WHERE id = $2",
        payload.status,
        order_id
    )
    .execute(state.write_pool())
    .await
    .map_err(|e| AppError::DatabaseError(e))?;

    let summary = fetch_single_summary(state.read_pool(), order_id).await?;
    Ok(Json(summary))
}

#[handler]
pub async fn delete_order(
    Data(state): Data<&AppState>,
    Path(order_id): Path<i32>,
) -> PoemResult<()> {
    let result = sqlx::query!("DELETE FROM orders WHERE id = $1", order_id)
        .execute(state.write_pool())
        .await
        .map_err(|e| AppError::DatabaseError(e))?;

    if result.rows_affected() == 0 {
        return Err(AppError::NotFound.into());
    }

    Ok(())
}

#[derive(sqlx::FromRow)]
struct DbOrderRow {
    order_id: i32,
    order_status: String,
    order_total: BigDecimal,
    order_created_at: chrono::DateTime<chrono::Utc>,
    order_item_id: Option<i32>,
    order_item_quantity: Option<i32>,
    order_item_price: Option<BigDecimal>,
    plant_id: Option<i32>,
    plant_name: Option<String>,
    plant_price: Option<BigDecimal>,
}

#[derive(sqlx::FromRow)]
struct InsertedOrderItemRow {
    id: i32,
    plant_id: i32,
    quantity: i32,
    price: BigDecimal,
}

async fn fetch_rows_for_user(pool: &PgPool, user_id: i32) -> Result<Vec<DbOrderRow>, AppError> {
    sqlx::query_as!(
        DbOrderRow,
        r#"
        SELECT
            o.id               AS order_id,
            o.status           AS order_status,
            o.total            AS order_total,
            o.created_at       AS order_created_at,
            oi.id              AS "order_item_id?",
            oi.quantity        AS "order_item_quantity?",
            oi.price           AS "order_item_price?",
            p.id               AS "plant_id?",
            p.name             AS "plant_name?",
            p.price            AS "plant_price?"
        FROM orders o
        LEFT JOIN order_items oi ON oi.order_id = o.id
        LEFT JOIN plants p ON p.id = oi.plant_id
        WHERE o.user_id = $1
        ORDER BY o.created_at DESC, oi.id ASC
        "#,
        user_id
    )
    .fetch_all(pool)
    .await
    .map_err(|e| {
        log_debug_lazy(|| format!("[orders] list_orders query failed for user {user_id}: {e}"));
        AppError::DatabaseError(e)
    })
}

async fn fetch_single_summary(pool: &PgPool, order_id: i32) -> Result<OrderSummary, AppError> {
    let rows = sqlx::query_as!(
        DbOrderRow,
        r#"
        SELECT
            o.id               AS order_id,
            o.status           AS order_status,
            o.total            AS order_total,
            o.created_at       AS order_created_at,
            oi.id              AS "order_item_id?",
            oi.quantity        AS "order_item_quantity?",
            oi.price           AS "order_item_price?",
            p.id               AS "plant_id?",
            p.name             AS "plant_name?",
            p.price            AS "plant_price?"
        FROM orders o
        LEFT JOIN order_items oi ON oi.order_id = o.id
        LEFT JOIN plants p ON p.id = oi.plant_id
        WHERE o.id = $1
        ORDER BY oi.id ASC
        "#,
        order_id
    )
    .fetch_all(pool)
    .await
    .map_err(|e| {
        log_debug_lazy(|| format!("[orders] summary query failed for order {order_id}: {e}"));
        AppError::DatabaseError(e)
    })?;

    fold_rows(rows).into_iter().next().ok_or(AppError::NotFound)
}

fn fold_rows(rows: Vec<DbOrderRow>) -> Vec<OrderSummary> {
    let mut summaries = Vec::new();
    let mut index = HashMap::new();

    for row in rows {
        let entry = index.entry(row.order_id).or_insert_with(|| {
            let summary = OrderSummary::new(
                row.order_id,
                row.order_status.clone(),
                row.order_total.as_i32_lossy(),
                row.order_created_at,
                Vec::new(),
            );
            summaries.push(summary);
            summaries.len() - 1
        });

        if let Some(item) = map_row_to_item(&row) {
            summaries[*entry].items.push(item);
        }
    }

    summaries
}

fn map_row_to_item(row: &DbOrderRow) -> Option<OrderItemResponse> {
    let item_id = row.order_item_id?;
    let plant_id = match row.plant_id {
        Some(id) => id,
        None => {
            log_debug_lazy(|| {
                format!(
                    "[orders] ignore order_item {:?} (order #{}) car la plante liée a été supprimée",
                    row.order_item_id, row.order_id
                )
            });
            return None;
        }
    };
    let item_price = row.order_item_price.clone()?;
    let plant_price = row.plant_price.clone()?;

    Some(OrderItemResponse::new(
        item_id,
        plant_id,
        row.order_item_quantity.unwrap_or(0),
        item_price.as_i32_lossy(),
        OrderItemPlant {
            id: plant_id,
            name: row.plant_name.clone().unwrap_or_default(),
            price: plant_price.as_i32_lossy(),
        },
    ))
}
