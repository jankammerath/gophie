//! Typed Gopher networking and content-mismatch error definitions.

use std::fmt;

use super::{GopherItemType, GopherUrl};

/// High-level error categories surfaced to the user.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum GopherError {
    InvalidUrl,
    HostUnknown,
    ConnectionTimeout,
    ConnectFailed,
    ParserFailed,
    UserCancelled,
    Exception(String),
}

impl fmt::Display for GopherError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidUrl => write!(f, "Invalid Gopher URL"),
            Self::HostUnknown => write!(f, "Server not found"),
            Self::ConnectionTimeout => write!(f, "Connection timed out"),
            Self::ConnectFailed => write!(f, "Connection refused"),
            Self::ParserFailed => write!(f, "Failed to parse Gopher response"),
            Self::UserCancelled => write!(f, "Cancelled by user"),
            Self::Exception(msg) => write!(f, "{}", msg),
        }
    }
}

/// Errors produced during a Gopher fetch operation.
#[derive(Debug, Clone)]
pub enum GopherFetchError {
    Network {
        error: GopherError,
        url: GopherUrl,
    },
    ItemMismatch {
        requested: GopherItemType,
        detected: GopherItemType,
        url: GopherUrl,
    },
}
