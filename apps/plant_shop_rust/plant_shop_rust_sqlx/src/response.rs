use bytes::{BufMut, BytesMut};
use poem::{http::StatusCode, Body, Response};
use serde::Serialize;
use std::io::{self, Write};
use std::cell::RefCell;

use crate::errors::AppError;

thread_local! {
    static JSON_BUFFER: RefCell<BytesMut> = RefCell::new(BytesMut::with_capacity(16 * 1024));
}

pub fn buffered_json<T: Serialize>(value: &T, status: StatusCode) -> Result<Response, AppError> {
    JSON_BUFFER.with(|buffer| {
        let mut buf = buffer.borrow_mut();
        buf.clear();
        let mut writer = BytesMutWriter { buf: &mut buf };
        serde_json::to_writer(&mut writer, value).map_err(|_| AppError::Internal)?;
        let body = Body::from_bytes(buf.split().freeze());
        Ok(Response::builder().status(status).body(body))
    })
}

struct BytesMutWriter<'a> {
    buf: &'a mut BytesMut,
}

impl<'a> Write for BytesMutWriter<'a> {
    fn write(&mut self, chunk: &[u8]) -> io::Result<usize> {
        self.buf.put_slice(chunk);
        Ok(chunk.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}
