//! Modeles DTO plantes.

// ==============================================================================
// Importations
// ==============================================================================

use bigdecimal::ToPrimitive;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize, Serializer};
use sqlx::types::BigDecimal;

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

/// Serialise un BigDecimal en i32 pour JSON.
///
/// @param value BigDecimal a convertir
/// @param serializer Serializer Serde
/// @return Resultat de serialisation
pub fn serialize_bigdecimal_as_i32<S>(value: &BigDecimal, serializer: S) -> Result<S::Ok, S::Error>
where
    S: Serializer,
{
    let nombre: i32 = value.to_i32().unwrap_or(0);
    serializer.serialize_i32(nombre)
}

pub trait PriceExt {
    fn as_i32_lossy(&self) -> i32;
}

// ==============================================================================
// Traits et Implementations
// ==============================================================================

impl PriceExt for BigDecimal {
    /// Convertit un BigDecimal en i32 (perte de precision).
    ///
    /// @return Prix en centimes (i32)
    fn as_i32_lossy(&self) -> i32 {
        self.to_i32().unwrap_or(0)
    }
}

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Serialize, Deserialize, sqlx::FromRow)]
/// Modele d'une plante (lecture DB).
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
/// DTO pour la creation d'une plante.
pub struct NewPlant {
    pub name: String,
    pub description: Option<String>,
    pub price: BigDecimal,
    pub stock: i32,
}

#[derive(Deserialize)]
/// DTO pour la mise a jour d'une plante (champs optionnels).
pub struct UpdatePlant {
    pub name: Option<String>,
    pub description: Option<String>,
    pub price: Option<BigDecimal>,
    pub stock: Option<i32>,
}
