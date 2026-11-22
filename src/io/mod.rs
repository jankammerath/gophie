//! Low-level I/O helpers, including magic-byte file signature detection.

mod file_signature;

pub use file_signature::{FileSignature, FileSignatureType};
