use bytes::Bytes;
use poem::{http::StatusCode, Body, Response};
use serde::Serialize;
use std::cell::RefCell;

use crate::errors::AppError;

thread_local! {
    static JSON_BUFFER: RefCell<Vec<u8>> = RefCell::new(Vec::with_capacity(16 * 1024));
}

pub fn buffered_json<T: Serialize>(value: &T, status: StatusCode) -> Result<Response, AppError> {
    JSON_BUFFER.with(|buffer| {
        let mut buf = buffer.borrow_mut();
        buf.clear();
        serde_json::to_writer(&mut *buf, value).map_err(|_| AppError::Internal)?;
        let body = Body::from_bytes(Bytes::from(buf.clone()));
        Ok(Response::builder().status(status).body(body))
    })
}
