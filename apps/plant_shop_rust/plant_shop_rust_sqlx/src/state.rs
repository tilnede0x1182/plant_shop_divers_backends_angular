use sqlx::PgPool;
use std::sync::Arc;

use crate::cache::{default_plant_cache, default_user_cache, SharedPlantCache, SharedUserCache};

#[derive(Clone)]
pub struct AppState {
    read_pool: PgPool,
    write_pool: PgPool,
    user_cache: SharedUserCache,
    plant_cache: SharedPlantCache,
}

impl AppState {
    pub fn new(pool: PgPool) -> Self {
        Self {
            read_pool: pool.clone(),
            write_pool: pool,
            user_cache: default_user_cache(),
            plant_cache: default_plant_cache(),
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
