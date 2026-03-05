//! Module de cache en memoire.

// ==============================================================================
// Importations
// ==============================================================================

use std::sync::Arc;
use std::time::{Duration, Instant};

use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::dto::{PlantResponse, UserResponse};

// ==============================================================================
// Traits
// ==============================================================================

#[async_trait]
/// Trait pour le cache utilisateurs.
pub trait UserCache: Send + Sync {
    async fn get(&self) -> Option<Vec<UserResponse>>;
    async fn set(&self, data: &[UserResponse]);
    async fn invalidate(&self);
}

#[async_trait]
/// Trait pour le cache plantes.
pub trait PlantCache: Send + Sync {
    async fn get(&self) -> Option<Vec<PlantResponse>>;
    async fn set(&self, data: &[PlantResponse]);
    async fn invalidate(&self);
}

// ==============================================================================
// Structures
// ==============================================================================

/// Cache avec expiration temporelle pour une liste d'elements.
struct TimedVecCache<T> {
    ttl: Duration,
    inner: RwLock<Option<(Instant, Vec<T>)>>,
}

impl<T> TimedVecCache<T> {
    fn new(ttl: Duration) -> Self {
        Self {
            ttl,
            inner: RwLock::new(None),
        }
    }
}

impl<T: Clone> TimedVecCache<T> {
    async fn get_fresh(&self) -> Option<Vec<T>> {
        let mut guard = self.inner.write().await;
        if let Some((stored_at, data)) = guard.as_ref() {
            if stored_at.elapsed() <= self.ttl {
                return Some(data.clone());
            }
        }
        guard.take();
        None
    }

    async fn set_snapshot(&self, data: &[T]) {
        self.inner
            .write()
            .await
            .replace((Instant::now(), data.to_vec()));
    }
}

impl<T> TimedVecCache<T> {
    async fn invalidate_inner(&self) {
        self.inner.write().await.take();
    }
}

// ==============================================================================
// Implementations
// ==============================================================================

#[async_trait]
impl UserCache for TimedVecCache<UserResponse> {
    /// Recupere les utilisateurs du cache si valide.
    async fn get(&self) -> Option<Vec<UserResponse>> {
        self.get_fresh().await
    }

    /// Met a jour le cache utilisateurs.
    async fn set(&self, data: &[UserResponse]) {
        self.set_snapshot(data).await;
    }

    /// Invalide le cache utilisateurs.
    async fn invalidate(&self) {
        self.invalidate_inner().await;
    }
}

#[async_trait]
impl PlantCache for TimedVecCache<PlantResponse> {
    /// Recupere les plantes du cache si valide.
    async fn get(&self) -> Option<Vec<PlantResponse>> {
        self.get_fresh().await
    }

    /// Met a jour le cache plantes.
    async fn set(&self, data: &[PlantResponse]) {
        self.set_snapshot(data).await;
    }

    /// Invalide le cache plantes.
    async fn invalidate(&self) {
        self.invalidate_inner().await;
    }
}

// ==============================================================================
// Types et Fonctions
// ==============================================================================

/// Cache partage pour les utilisateurs.
pub type SharedUserCache = Arc<dyn UserCache>;
/// Cache partage pour les plantes.
pub type SharedPlantCache = Arc<dyn PlantCache>;

/// Cree un cache utilisateurs avec le TTL specifie.
pub fn default_user_cache(ttl: Duration) -> SharedUserCache {
    Arc::new(TimedVecCache::new(ttl))
}

/// Cree un cache plantes avec le TTL specifie.
pub fn default_plant_cache(ttl: Duration) -> SharedPlantCache {
    Arc::new(TimedVecCache::new(ttl))
}
