//! Gopher protocol networking (RFC 1436 & RFC 4266), page models, and download management.

mod download;
mod gopher_client;
mod gopher_error;
mod gopher_item;
mod gopher_page;
mod gopher_url;

pub use download::{DownloadItem, DownloadManager, DownloadStatus};
pub use gopher_client::{FetchEvent, GopherClient};
pub use gopher_error::{GopherError, GopherFetchError};
pub use gopher_item::{GopherItem, GopherItemType};
pub use gopher_page::GopherPage;
pub use gopher_url::GopherUrl;
