//! Asynchronous TCP Gopher client with streaming progress, file-signature detection, and cancellation.

use std::fs::{self, File};
use std::io::{self, ErrorKind, Read, Write};
use std::net::{TcpStream, ToSocketAddrs};
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc};
use std::thread;
use std::time::{Duration, Instant};

use crate::io::{FileSignature, FileSignatureType};

use super::{GopherError, GopherFetchError, GopherItemType, GopherPage, GopherUrl};

const CONNECT_TIMEOUT: Duration = Duration::from_secs(12);
const READ_POLL_TIMEOUT: Duration = Duration::from_millis(250);
const IDLE_READ_TIMEOUT: Duration = Duration::from_secs(20);
const CHUNK_SIZE: usize = 16 * 1024;
const LARGE_TEXT_CHECK_THRESHOLD: u64 = 200_000;

/// Events emitted by an asynchronous page fetch to the UI thread.
#[derive(Debug)]
pub enum FetchEvent {
    Progress {
        request_id: u64,
        url: GopherUrl,
        bytes_loaded: u64,
    },
    Loaded {
        request_id: u64,
        page: GopherPage,
    },
    Failed {
        request_id: u64,
        error: GopherError,
        url: GopherUrl,
    },
    ItemMismatch {
        request_id: u64,
        requested: GopherItemType,
        detected: GopherItemType,
        url: GopherUrl,
    },
}

/// Progress and completion events emitted by a background file download.
#[derive(Debug)]
pub enum DownloadEvent {
    Progress {
        id: u64,
        bytes_loaded: u64,
        bytes_per_sec: u64,
    },
    Completed {
        id: u64,
        bytes_loaded: u64,
    },
    Failed {
        id: u64,
        message: String,
    },
}

/// Manages active Gopher network requests and cancellation state.
pub struct GopherClient {
    active_cancel: Option<Arc<AtomicBool>>,
    next_request_id: u64,
}

impl Default for GopherClient {
    fn default() -> Self {
        Self::new()
    }
}

impl GopherClient {
    /// Creates a new `GopherClient`.
    pub fn new() -> Self {
        Self {
            active_cancel: None,
            next_request_id: 1,
        }
    }

    /// Cancels the currently active page fetch, if any.
    pub fn cancel_fetch(&mut self) {
        if let Some(flag) = self.active_cancel.take() {
            flag.store(true, Ordering::SeqCst);
        }
    }

    /// Fetches a Gopher page, text document, or image asynchronously on a background thread.
    /// Returns the assigned request ID so stale responses can be ignored.
    pub fn fetch_async<F>(
        &mut self,
        url_str: String,
        content_type: GopherItemType,
        charset: String,
        tx: mpsc::Sender<FetchEvent>,
        on_update: F,
    ) -> u64
    where
        F: Fn() + Send + 'static,
    {
        self.cancel_fetch();

        let request_id = self.next_request_id;
        self.next_request_id += 1;

        let cancel_flag = Arc::new(AtomicBool::new(false));
        self.active_cancel = Some(Arc::clone(&cancel_flag));

        thread::spawn(move || {
            let gopher_url = GopherUrl::parse(&url_str);
            let result = Self::fetch_blocking(
                request_id,
                &gopher_url,
                content_type,
                &charset,
                &cancel_flag,
                &tx,
                &on_update,
            );

            if cancel_flag.load(Ordering::SeqCst) {
                return;
            }

            match result {
                Ok(page) => {
                    let _ = tx.send(FetchEvent::Loaded { request_id, page });
                }
                Err(GopherFetchError::Network { error, url }) => {
                    let _ = tx.send(FetchEvent::Failed {
                        request_id,
                        error,
                        url,
                    });
                }
                Err(GopherFetchError::ItemMismatch {
                    requested,
                    detected,
                    url,
                }) => {
                    let _ = tx.send(FetchEvent::ItemMismatch {
                        request_id,
                        requested,
                        detected,
                        url,
                    });
                }
            }
            on_update();
        });

        request_id
    }

    /// Streams a Gopher resource directly to `target_path` on a background thread.
    pub fn download_async<F>(
        id: u64,
        url_str: String,
        target_path: PathBuf,
        cancel_flag: Arc<AtomicBool>,
        tx: mpsc::Sender<DownloadEvent>,
        on_update: F,
    ) where
        F: Fn() + Send + 'static,
    {
        thread::spawn(move || {
            let gopher_url = GopherUrl::parse(&url_str);
            let outcome = Self::download_blocking(
                id,
                &gopher_url,
                &target_path,
                &cancel_flag,
                &tx,
                &on_update,
            );

            if cancel_flag.load(Ordering::SeqCst) {
                let _ = fs::remove_file(&target_path);
                return;
            }

            match outcome {
                Ok(bytes_loaded) => {
                    let _ = tx.send(DownloadEvent::Completed { id, bytes_loaded });
                }
                Err(message) => {
                    let _ = fs::remove_file(&target_path);
                    let _ = tx.send(DownloadEvent::Failed { id, message });
                }
            }
            on_update();
        });
    }

    fn connect_and_request(url: &GopherUrl) -> Result<TcpStream, GopherError> {
        if url.host().is_empty() {
            return Err(GopherError::InvalidUrl);
        }

        let addr_str = format!("{}:{}", url.host(), url.port());
        let mut addrs = addr_str.to_socket_addrs().map_err(|_| GopherError::HostUnknown)?;
        let Some(first_addr) = addrs.next() else {
            return Err(GopherError::HostUnknown);
        };

        let mut last_err = None;
        let mut stream_opt = None;

        for addr in std::iter::once(first_addr).chain(addrs) {
            match TcpStream::connect_timeout(&addr, CONNECT_TIMEOUT) {
                Ok(stream) => {
                    stream_opt = Some(stream);
                    break;
                }
                Err(err) => last_err = Some(err),
            }
        }

        let mut stream = match stream_opt {
            Some(s) => s,
            None => {
                let err = last_err.unwrap_or_else(|| io::Error::other("connection failed"));
                return Err(Self::map_io_error(&err));
            }
        };

        let _ = stream.set_read_timeout(Some(READ_POLL_TIMEOUT));
        let _ = stream.set_write_timeout(Some(CONNECT_TIMEOUT));

        let request = format!("{}\r\n", url.wire_selector());
        stream
            .write_all(request.as_bytes())
            .map_err(|e| Self::map_io_error(&e))?;
        stream.flush().map_err(|e| Self::map_io_error(&e))?;

        Ok(stream)
    }

    fn fetch_blocking<F>(
        request_id: u64,
        url: &GopherUrl,
        mut content_type: GopherItemType,
        charset: &str,
        cancel_flag: &AtomicBool,
        tx: &mpsc::Sender<FetchEvent>,
        on_update: &F,
    ) -> Result<GopherPage, GopherFetchError>
    where
        F: Fn(),
    {
        let mut stream = Self::connect_and_request(url).map_err(|error| {
            GopherFetchError::Network {
                error,
                url: url.clone(),
            }
        })?;

        let mut buffer = Vec::with_capacity(CHUNK_SIZE);
        let mut chunk = [0u8; CHUNK_SIZE];
        let mut total_bytes: u64 = 0;
        let mut last_data_at = Instant::now();
        let mut checked_large_text = false;

        loop {
            if cancel_flag.load(Ordering::SeqCst) {
                return Err(GopherFetchError::Network {
                    error: GopherError::UserCancelled,
                    url: url.clone(),
                });
            }

            match stream.read(&mut chunk) {
                Ok(0) => break,
                Ok(read_len) => {
                    last_data_at = Instant::now();
                    let slice = &chunk[..read_len];

                    // Inspect magic header on the first chunk received.
                    if total_bytes == 0 {
                        let sig = FileSignature::new(slice).detect();
                        match sig {
                            FileSignatureType::Image if !content_type.is_image() => {
                                content_type = GopherItemType::ImageFile;
                            }
                            FileSignatureType::Media => {
                                return Err(GopherFetchError::ItemMismatch {
                                    requested: content_type,
                                    detected: GopherItemType::SoundFile,
                                    url: url.clone(),
                                });
                            }
                            FileSignatureType::Binary => {
                                return Err(GopherFetchError::ItemMismatch {
                                    requested: content_type,
                                    detected: GopherItemType::BinaryFile,
                                    url: url.clone(),
                                });
                            }
                            _ => {}
                        }
                    }

                    // Guard against mislabeled large binary payloads sent as text/menu.
                    if !checked_large_text
                        && total_bytes > LARGE_TEXT_CHECK_THRESHOLD
                        && matches!(
                            content_type,
                            GopherItemType::GopherMenu
                                | GopherItemType::TextFile
                                | GopherItemType::Unknown
                        )
                    {
                        checked_large_text = true;
                        if FileSignature::new(slice).detect() != FileSignatureType::Text {
                            return Err(GopherFetchError::ItemMismatch {
                                requested: content_type,
                                detected: GopherItemType::BinaryFile,
                                url: url.clone(),
                            });
                        }
                    }

                    buffer.extend_from_slice(slice);
                    total_bytes += read_len as u64;

                    let _ = tx.send(FetchEvent::Progress {
                        request_id,
                        url: url.clone(),
                        bytes_loaded: total_bytes,
                    });
                    on_update();
                }
                Err(err)
                    if matches!(err.kind(), ErrorKind::WouldBlock | ErrorKind::TimedOut) =>
                {
                    if last_data_at.elapsed() >= IDLE_READ_TIMEOUT {
                        return Err(GopherFetchError::Network {
                            error: GopherError::ConnectionTimeout,
                            url: url.clone(),
                        });
                    }
                }
                Err(err) => {
                    return Err(GopherFetchError::Network {
                        error: Self::map_io_error(&err),
                        url: url.clone(),
                    });
                }
            }
        }

        Ok(GopherPage::new(buffer, content_type, url.clone(), charset))
    }

    fn download_blocking<F>(
        id: u64,
        url: &GopherUrl,
        target_path: &PathBuf,
        cancel_flag: &AtomicBool,
        tx: &mpsc::Sender<DownloadEvent>,
        on_update: &F,
    ) -> Result<u64, String>
    where
        F: Fn(),
    {
        if let Some(parent) = target_path.parent() {
            fs::create_dir_all(parent).map_err(|e| e.to_string())?;
        }
        let mut file = File::create(target_path).map_err(|e| e.to_string())?;
        let mut stream = Self::connect_and_request(url).map_err(|e| e.to_string())?;

        let mut chunk = [0u8; CHUNK_SIZE];
        let mut total_bytes: u64 = 0;
        let started_at = Instant::now();
        let mut last_report = Instant::now();
        let mut last_data_at = Instant::now();

        loop {
            if cancel_flag.load(Ordering::SeqCst) {
                return Err("Cancelled".to_string());
            }

            match stream.read(&mut chunk) {
                Ok(0) => break,
                Ok(read_len) => {
                    last_data_at = Instant::now();
                    file.write_all(&chunk[..read_len])
                        .map_err(|e| e.to_string())?;
                    total_bytes += read_len as u64;

                    // Throttle UI notifications to ~10 Hz to keep download overhead near zero.
                    if last_report.elapsed() >= Duration::from_millis(100) {
                        let elapsed = started_at.elapsed().as_secs_f64().max(0.001);
                        let bytes_per_sec = (total_bytes as f64 / elapsed) as u64;
                        let _ = tx.send(DownloadEvent::Progress {
                            id,
                            bytes_loaded: total_bytes,
                            bytes_per_sec,
                        });
                        on_update();
                        last_report = Instant::now();
                    }
                }
                Err(err)
                    if matches!(err.kind(), ErrorKind::WouldBlock | ErrorKind::TimedOut) =>
                {
                    if last_data_at.elapsed() >= IDLE_READ_TIMEOUT {
                        return Err("Connection timed out".to_string());
                    }
                }
                Err(err) => return Err(err.to_string()),
            }
        }

        file.flush().map_err(|e| e.to_string())?;
        Ok(total_bytes)
    }

    fn map_io_error(err: &io::Error) -> GopherError {
        match err.kind() {
            ErrorKind::ConnectionRefused => GopherError::ConnectFailed,
            ErrorKind::TimedOut => GopherError::ConnectionTimeout,
            _ => GopherError::Exception(err.to_string()),
        }
    }
}
