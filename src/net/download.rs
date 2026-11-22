//! Parallel file download manager tracking active, completed, and failed transfers.

use std::fs;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc};

use crate::config::SystemUtility;

use super::gopher_client::{DownloadEvent, GopherClient};
use super::{GopherItem, GopherUrl};

/// Lifecycle state of an individual download.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DownloadStatus {
    Idle,
    Active,
    Completed,
    Failed,
}

/// Represents a single file download from Gopherspace.
#[derive(Debug)]
pub struct DownloadItem {
    id: u64,
    item: GopherItem,
    target_path: PathBuf,
    open_when_finished: bool,
    status: DownloadStatus,
    bytes_loaded: u64,
    bytes_per_sec: u64,
    cancel_flag: Arc<AtomicBool>,
}

impl DownloadItem {
    pub fn id(&self) -> u64 {
        self.id
    }

    pub fn gopher_item(&self) -> &GopherItem {
        &self.item
    }

    pub fn target_path(&self) -> &Path {
        &self.target_path
    }

    pub fn status(&self) -> DownloadStatus {
        self.status
    }

    pub fn bytes_loaded(&self) -> u64 {
        self.bytes_loaded
    }

    pub fn bytes_per_sec(&self) -> u64 {
        self.bytes_per_sec
    }

    /// Formats the secondary status line shown in the Downloads window.
    pub fn status_summary(&self) -> String {
        let size = SystemUtility::format_file_size(self.bytes_loaded);
        let detail = match self.status {
            DownloadStatus::Idle => String::from("Download not started"),
            DownloadStatus::Active => {
                let rate = SystemUtility::format_file_size(self.bytes_per_sec);
                format!("{} ({}/sec)", size, rate)
            }
            DownloadStatus::Completed => format!("Completed ({})", size),
            DownloadStatus::Failed => format!("Failed ({})", size),
        };
        format!("{} — {}", detail, self.item.host())
    }

    /// Opens the downloaded file with the OS default application.
    pub fn open_on_desktop(&self) {
        if let Err(err) = SystemUtility::open_file(&self.target_path) {
            eprintln!("{}", err);
        }
    }
}

/// Manages concurrent downloads, polls progress events, and persists history across restarts.
pub struct DownloadManager {
    items: Vec<DownloadItem>,
    next_id: u64,
    tx: mpsc::Sender<DownloadEvent>,
    rx: mpsc::Receiver<DownloadEvent>,
    history_path: Option<PathBuf>,
}

impl Default for DownloadManager {
    fn default() -> Self {
        Self::new()
    }
}

impl DownloadManager {
    /// Creates an empty in-memory `DownloadManager`.
    pub fn new() -> Self {
        let (tx, rx) = mpsc::channel();
        Self {
            items: Vec::new(),
            next_id: 1,
            tx,
            rx,
            history_path: None,
        }
    }

    /// Creates a `DownloadManager` backed by a persistent history file and loads existing entries.
    pub fn with_history_path(path: PathBuf) -> Self {
        let mut manager = Self::new();
        manager.history_path = Some(path);
        manager.load_history();
        manager
    }

    /// Starts downloading `item` to `target_path` in a background thread.
    pub fn start_download<F>(
        &mut self,
        item: GopherItem,
        target_path: PathBuf,
        open_when_finished: bool,
        on_update: F,
    ) -> u64
    where
        F: Fn() + Send + 'static,
    {
        let id = self.next_id;
        self.next_id += 1;

        let cancel_flag = Arc::new(AtomicBool::new(false));
        let url = GopherUrl::parse(&item.url_string()).to_url_string(false);

        GopherClient::download_async(
            id,
            url,
            target_path.clone(),
            Arc::clone(&cancel_flag),
            self.tx.clone(),
            on_update,
        );

        self.items.push(DownloadItem {
            id,
            item,
            target_path,
            open_when_finished,
            status: DownloadStatus::Active,
            bytes_loaded: 0,
            bytes_per_sec: 0,
            cancel_flag,
        });

        id
    }

    /// Retries a failed or idle download by ID.
    pub fn retry<F>(&mut self, id: u64, on_update: F)
    where
        F: Fn() + Send + 'static,
    {
        if let Some(entry) = self.items.iter_mut().find(|d| d.id == id) {
            entry.cancel_flag = Arc::new(AtomicBool::new(false));
            entry.bytes_loaded = 0;
            entry.bytes_per_sec = 0;
            entry.status = DownloadStatus::Active;

            let url = GopherUrl::parse(&entry.item.url_string()).to_url_string(false);
            GopherClient::download_async(
                id,
                url,
                entry.target_path.clone(),
                Arc::clone(&entry.cancel_flag),
                self.tx.clone(),
                on_update,
            );
        }
    }

    /// Cancels an active download, removes it from the list, and deletes its partial file.
    pub fn abort_and_remove(&mut self, id: u64) {
        if let Some(pos) = self.items.iter().position(|d| d.id == id) {
            let entry = self.items.remove(pos);
            entry.cancel_flag.store(true, Ordering::SeqCst);
            let _ = fs::remove_file(&entry.target_path);
            self.save_history();
        }
    }

    /// Removes all completed, failed, and idle items while keeping active downloads.
    pub fn clear_non_active(&mut self) {
        self.items.retain(|d| d.status == DownloadStatus::Active);
        self.save_history();
    }

    /// Returns `true` if there is at least one non-active item that can be cleared.
    pub fn has_non_active(&self) -> bool {
        self.items.iter().any(|d| d.status != DownloadStatus::Active)
    }

    /// Returns the number of currently active downloads.
    pub fn active_count(&self) -> usize {
        self.items
            .iter()
            .filter(|d| d.status == DownloadStatus::Active)
            .count()
    }

    /// Returns all downloads in newest-first order for display.
    pub fn items_newest_first(&self) -> impl Iterator<Item = &DownloadItem> {
        self.items.iter().rev()
    }

    /// Looks up a download item by ID.
    pub fn get(&self, id: u64) -> Option<&DownloadItem> {
        self.items.iter().find(|d| d.id == id)
    }

    /// Drains pending background download events and updates item state.
    pub fn poll_events(&mut self) {
        let mut changed = false;

        while let Ok(event) = self.rx.try_recv() {
            match event {
                DownloadEvent::Progress {
                    id,
                    bytes_loaded,
                    bytes_per_sec,
                } => {
                    if let Some(entry) = self.items.iter_mut().find(|d| d.id == id) {
                        entry.bytes_loaded = bytes_loaded;
                        entry.bytes_per_sec = bytes_per_sec;
                    }
                }
                DownloadEvent::Completed { id, bytes_loaded } => {
                    if let Some(entry) = self.items.iter_mut().find(|d| d.id == id) {
                        entry.bytes_loaded = bytes_loaded;
                        entry.status = DownloadStatus::Completed;
                        if entry.open_when_finished {
                            entry.open_on_desktop();
                        }
                        changed = true;
                    }
                }
                DownloadEvent::Failed { id, message } => {
                    eprintln!("Download #{} failed: {}", id, message);
                    if let Some(entry) = self.items.iter_mut().find(|d| d.id == id) {
                        entry.status = DownloadStatus::Failed;
                        changed = true;
                    }
                }
            }
        }

        if changed {
            self.save_history();
        }
    }

    fn save_history(&self) {
        let Some(path) = &self.history_path else {
            return;
        };

        let mut content = String::new();
        for entry in &self.items {
            let status_str = match entry.status {
                DownloadStatus::Completed => "completed",
                DownloadStatus::Failed => "failed",
                DownloadStatus::Active | DownloadStatus::Idle => continue,
            };
            let clean = |s: &str| s.replace(['\t', '\r', '\n'], " ");
            let line = format!(
                "{}\t{}\t{}\t{}\t{}\t{}\t{}\n",
                status_str,
                entry.bytes_loaded,
                entry.item.item_type_code(),
                clean(entry.item.host()),
                entry.item.port(),
                clean(entry.item.selector()),
                clean(&entry.target_path.to_string_lossy()),
            );
            content.push_str(&line);
        }

        if let Some(parent) = path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        if let Err(err) = fs::write(path, content) {
            eprintln!("Failed to save download history: {}", err);
        }
    }

    fn load_history(&mut self) {
        let Some(path) = &self.history_path else {
            return;
        };
        let Ok(content) = fs::read_to_string(path) else {
            return;
        };

        for line in content.lines() {
            let cols: Vec<&str> = line.split('\t').collect();
            if cols.len() < 7 {
                continue;
            }
            let status = match cols[0] {
                "completed" => DownloadStatus::Completed,
                "failed" => DownloadStatus::Failed,
                _ => continue,
            };
            let bytes_loaded = cols[1].parse::<u64>().unwrap_or(0);
            let type_code = cols[2].chars().next().unwrap_or('9');
            let host = cols[3];
            let port = cols[4].parse::<u16>().unwrap_or(70);
            let selector = cols[5];
            let target_path = PathBuf::from(cols[6]);

            let url = GopherUrl::from_parts(host, port, selector);
            let item = GopherItem::from_code_and_url(type_code, &url);
            let id = self.next_id;
            self.next_id += 1;

            self.items.push(DownloadItem {
                id,
                item,
                target_path,
                open_when_finished: false,
                status,
                bytes_loaded,
                bytes_per_sec: 0,
                cancel_flag: Arc::new(AtomicBool::new(false)),
            });
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn persists_and_reloads_download_history() {
        let tmp = std::env::temp_dir().join("gophie_test_downloads_history.tsv");
        let _ = fs::remove_file(&tmp);

        let mut mgr = DownloadManager::with_history_path(tmp.clone());
        let url = GopherUrl::from_parts("gopher.semmel.ch", 70, "/audio/test.mp3");
        let item = GopherItem::from_code_and_url('s', &url);
        mgr.items.push(DownloadItem {
            id: 1,
            item,
            target_path: PathBuf::from("/tmp/test.mp3"),
            open_when_finished: false,
            status: DownloadStatus::Completed,
            bytes_loaded: 1_234_567,
            bytes_per_sec: 0,
            cancel_flag: Arc::new(AtomicBool::new(false)),
        });
        mgr.save_history();

        let reloaded = DownloadManager::with_history_path(tmp.clone());
        let entries: Vec<&DownloadItem> = reloaded.items_newest_first().collect();
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].status(), DownloadStatus::Completed);
        assert_eq!(entries[0].bytes_loaded(), 1_234_567);
        assert_eq!(entries[0].gopher_item().file_name(), "test.mp3");
        assert_eq!(entries[0].gopher_item().host(), "gopher.semmel.ch");
        assert_eq!(entries[0].target_path(), Path::new("/tmp/test.mp3"));

        let _ = fs::remove_file(&tmp);
    }
}
