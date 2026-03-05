//! Etat partage de l'application.

// ==============================================================================
// Importations
// ==============================================================================

use sqlx::PgPool;
use std::sync::Arc;
use std::time::Duration;

use crate::cache::{default_plant_cache, default_user_cache, SharedPlantCache, SharedUserCache};

// ==============================================================================
// Structures
// ==============================================================================

/// Configuration des TTL de cache.
#[derive(Clone, Copy)]
pub struct CacheTtls {
    pub users: Duration,
    pub plants: Duration,
}

impl CacheTtls {
    /// Cree une nouvelle configuration de TTL.
    ///
    /// @param users TTL du cache utilisateurs
    /// @param plants TTL du cache plantes
    /// @return CacheTtls
    pub fn new(users: Duration, plants: Duration) -> Self {
        Self { users, plants }
    }
}

// ==============================================================================
// AppState
// ==============================================================================

#[derive(Clone)]
/// Etat partage de l'application (pools DB, caches).
pub struct AppState {
    read_pool: PgPool,
    write_pool: PgPool,
    user_cache: SharedUserCache,
    plant_cache: SharedPlantCache,
}

impl AppState {
    /// Cree un nouvel AppState avec les pools et TTL de cache.
    ///
    /// @param read_pool Pool de lecture
    /// @param write_pool Pool d'ecriture
    /// @param cache_ttls Configuration des TTL
    /// @return AppState
    pub fn new(read_pool: PgPool, write_pool: PgPool, cache_ttls: CacheTtls) -> Self {
        Self::with_caches(
            read_pool,
            write_pool,
            default_user_cache(cache_ttls.users),
            default_plant_cache(cache_ttls.plants),
        )
    }

    pub fn with_caches(
        read_pool: PgPool,
        write_pool: PgPool,
        user_cache: SharedUserCache,
        plant_cache: SharedPlantCache,
    ) -> Self {
        Self {
            read_pool,
            write_pool,
            user_cache,
            plant_cache,
        }
    }

    pub fn read_pool(&self) -> &PgPool {
        &self.read_pool
    }

    pub fn write_pool(&self) -> &PgPool {
        &self.write_pool
    }

    pub fn user_cache(&self) -> SharedUserCache {
        Arc::clone(&self.user_cache)
    }

    pub fn plant_cache(&self) -> SharedPlantCache {
        Arc::clone(&self.plant_cache)
    }
}
