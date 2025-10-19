use serde::{Serialize, Deserialize, Serializer};
use sea_orm::prelude::Decimal;
use chrono::{DateTime, Utc};
use num_traits::ToPrimitive;

/// Sérialise un Decimal en i32 (pour l’API)
pub fn serialize_decimal_as_i32<S>(value: &Decimal, serializer: S) -> Result<S::Ok, S::Error>
where
	S: Serializer
{
	let nombre: i32 = value.to_i32().unwrap_or(0);
	serializer.serialize_i32(nombre)
}

#[derive(Serialize, Deserialize, Clone)]
pub struct Plant {
	pub id: i32,
	pub name: String,
	#[serde(serialize_with = "serialize_decimal_as_i32")]
	pub price: Decimal,
	pub stock: i32,
	pub description: Option<String>,
	pub created_at: DateTime<Utc>,
}

#[derive(Deserialize)]
pub struct NewPlant {
	pub name: String,
	pub description: Option<String>,
	pub price: Decimal,
	pub stock: i32,
}

#[derive(Deserialize)]
pub struct UpdatePlant {
	pub name: Option<String>,
	pub description: Option<String>,
	pub price: Option<Decimal>,
	pub stock: Option<i32>,
}
