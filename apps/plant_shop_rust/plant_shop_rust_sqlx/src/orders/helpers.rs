use poem::web::cookie::CookieJar;
use sqlx::types::BigDecimal;
use sqlx::PgPool;

use crate::auth::jwt::verify_jwt;
use crate::errors::AppError;

use super::models::{OrderItemWithPlant, PlantBasic};

/// Récupère l'identifiant utilisateur depuis le cookie JWT ou via fallback DB.
pub async fn resolve_user_id(jar: &CookieJar, pool: &PgPool) -> Result<i32, AppError> {
    if let Some(c) = jar.get("auth_token") {
        let token = c.value_str();
        let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
        let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
        Ok(claims.sub)
    } else {
        let row = sqlx::query!(
            "SELECT id FROM users WHERE is_admin = false ORDER BY created_at DESC LIMIT 1"
        )
        .fetch_one(pool)
        .await
        .map_err(AppError::DatabaseError)?;
        Ok(row.id)
    }
}

/// Construit la structure enrichie OrderItemWithPlant à partir d'un plant minimal.
pub fn build_order_item_with_plant(
    id: i32,
    quantity: i32,
    price: BigDecimal,
    plant: PlantBasic,
) -> OrderItemWithPlant {
    OrderItemWithPlant {
        id,
        quantity,
        price,
        plant_id: plant.id,
        plant,
    }
}
