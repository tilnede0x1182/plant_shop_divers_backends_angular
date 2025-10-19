use serde::{Serialize, Deserialize, Serializer};
use sqlx::types::BigDecimal;
use chrono::{DateTime, Utc};
use bigdecimal::ToPrimitive;

pub fn serialize_bigdecimal_as_i32<S>(value: &BigDecimal, serializer: S) -> Result<S::Ok, S::Error>
where
	S: Serializer
{
	let nombre: i32 = value.to_i32().unwrap_or(0);
	serializer.serialize_i32(nombre)
}

#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct Plant {
	pub id: i32,
	pub name: String,
	#[serde(serialize_with = "serialize_bigdecimal_as_i32")]
	pub price: BigDecimal,
	pub stock: i32,
	pub description: Option<String>,
	pub created_at: DateTime<Utc>,
}

#[derive(Deserialize)]
pub struct NewPlant {
	pub name: String,
	pub description: Option<String>,
	pub price: BigDecimal,
	pub stock: i32,
}

#[derive(Deserialize)]
pub struct UpdatePlant {
	pub name: Option<String>,
	pub description: Option<String>,
	pub price: Option<BigDecimal>,
	pub stock: Option<i32>,
}
