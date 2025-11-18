use sqlx::PgPool;
use std::sync::Arc;
use std::time::Duration;

use crate::cache::{default_plant_cache, default_user_cache, SharedPlantCache, SharedUserCache};

#[derive(Clone, Copy)]
pub struct CacheTtls {
    pub users: Duration,
    pub plants: Duration,
}

impl CacheTtls {
    pub fn new(users: Duration, plants: Duration) -> Self {
        Self { users, plants }
    }
}

#[derive(Clone)]
pub struct AppState {
    read_pool: PgPool,
    write_pool: PgPool,
    user_cache: SharedUserCache,
    plant_cache: SharedPlantCache,
}

impl AppState {
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
