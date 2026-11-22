//! Main browser window coordinating navigation, history, network events, and sub-views.

use std::sync::mpsc;

use eframe::egui::{self, Key, Modifiers, ViewportCommand};

use crate::config::{ConfigManager, SystemUtility, WindowAppearance};
use crate::net::{
    CapsCache, CapsEvent, DownloadManager, FetchEvent, GopherCaps, GopherClient, GopherError,
    GopherItem, GopherItemType, GopherPage, GopherUrl,
};

use super::assets;
use super::download_window::{DownloadWindow, DownloadWindowAction};
use super::message_view::{ConfirmAction, MessageOutcome, MessageView};
use super::navigation_bar::{NavAction, NavigationBar};
use super::page_menu::{PageMenu, PageMenuAction};
use super::page_view::{PageView, PageViewAction};
use super::search_input::SearchInput;
use super::server_info_window::{ServerInfoAction, ServerInfoWindow};
use super::settings_window::SettingsWindow;

/// Application display name used in window titles.
pub const APPLICATION_TITLE: &str = "Gophie";

/// Top-level `eframe::App` encapsulating Gophie's browser state and UI components.
pub struct MainWindow {
    config: ConfigManager,
    client: GopherClient,
    downloads: DownloadManager,

    fetch_tx: mpsc::Sender<FetchEvent>,
    fetch_rx: mpsc::Receiver<FetchEvent>,
    active_request_id: u64,

    caps_cache: CapsCache,
    caps_tx: mpsc::Sender<CapsEvent>,
    caps_rx: mpsc::Receiver<CapsEvent>,

    history: Vec<GopherPage>,
    history_pos: Option<usize>,

    navigation_bar: NavigationBar,
    page_view: PageView,
    message_view: MessageView,
    search_input: SearchInput,
    download_window: DownloadWindow,
    settings_window: SettingsWindow,
    server_info_window: ServerInfoWindow,

    initial_load_started: bool,
    applied_window_appearance: Option<WindowAppearance>,
}

impl MainWindow {
    /// Creates and configures the `MainWindow` inside the `eframe` creation callback.
    pub fn new(cc: &eframe::CreationContext<'_>) -> Self {
        assets::configure_fonts(&cc.egui_ctx);

        let config = ConfigManager::load_default();
        let home = config.gopher_home();
        let (fetch_tx, fetch_rx) = mpsc::channel();
        let (caps_tx, caps_rx) = mpsc::channel();

        Self {
            config,
            client: GopherClient::new(),
            downloads: DownloadManager::new(),
            fetch_tx,
            fetch_rx,
            active_request_id: 0,
            caps_cache: CapsCache::new(),
            caps_tx,
            caps_rx,
            history: Vec::new(),
            history_pos: None,
            navigation_bar: NavigationBar::new(home),
            page_view: PageView::new(),
            message_view: MessageView::new(),
            search_input: SearchInput::new(),
            download_window: DownloadWindow::new(),
            settings_window: SettingsWindow::new(),
            server_info_window: ServerInfoWindow::new(),
            initial_load_started: false,
            applied_window_appearance: None,
        }
    }

    /// Formats a URL for display in the navigation bar, honoring `SELECTOR_PREFIX_ENABLED`.
    fn format_display_address(&self, url: &GopherUrl, item_type: GopherItemType) -> String {
        if self.config.selector_prefix_enabled() {
            let mut prefixed = url.clone();
            prefixed.set_type_prefix(item_type.code());
            prefixed.to_url_string(true)
        } else {
            url.to_url_string(false)
        }
    }

    /// Starts an asynchronous Gopher fetch for `address` with expected `content_type`.
    fn fetch_gopher_content(
        &mut self,
        ctx: &egui::Context,
        address: &str,
        content_type: GopherItemType,
    ) {
        self.navigation_bar.set_is_loading(true);
        let parsed = GopherUrl::parse(address);
        self.ensure_caps_fetched(ctx, parsed.host(), parsed.port());
        let display = self.format_display_address(&parsed, content_type);
        self.navigation_bar.set_address_text(display);

        let repaint_ctx = ctx.clone();
        self.active_request_id = self.client.fetch_async(
            address.to_string(),
            content_type,
            self.config.default_charset(),
            self.fetch_tx.clone(),
            move || repaint_ctx.request_repaint(),
        );
    }

    /// Handles navigation requests from links or the address bar.
    fn handle_address_request(&mut self, ctx: &egui::Context, address: &str, item: GopherItem) {
        if item.is_binary_file() {
            self.confirm_download(item);
            return;
        }

        match item.item_type() {
            GopherItemType::FullTextSearch => {
                self.search_input.open(item.display_text(), address);
            }
            GopherItemType::CcsoNameserver
            | GopherItemType::Telnet
            | GopherItemType::Telnet3270 => {
                let host = item.host().to_string();
                let port = item.port();
                self.message_view.show_confirm(
                    format!("Open a Telnet session with \"{}:{}\"?", host, port),
                    &["Open Telnet", "Dismiss"],
                    ConfirmAction::OpenTelnet { host, port },
                );
            }
            _ => {
                if address.starts_with("https://") || address.starts_with("http://") {
                    self.message_view.show_confirm(
                        format!("Open \"{}\" with your web browser?", address),
                        &["Open Website", "Dismiss"],
                        ConfirmAction::OpenWeb(address.to_string()),
                    );
                } else if let Some(email) = address.strip_prefix("mailto:") {
                    self.message_view.show_confirm(
                        format!("Do you want to send an e-mail to \"{}\"?", email),
                        &["Create new e-mail", "Dismiss"],
                        ConfirmAction::OpenEmail(email.to_string()),
                    );
                } else {
                    let clean_url = GopherUrl::parse(address).to_url_string(false);
                    self.fetch_gopher_content(ctx, &clean_url, item.item_type());
                }
            }
        }
    }

    fn confirm_download(&mut self, item: GopherItem) {
        let prompt = format!(
            "Download \"{}\" from \"{}\"?",
            item.file_name(),
            item.host()
        );
        self.message_view.show_confirm(
            prompt,
            &["Open", "Save", "Dismiss"],
            ConfirmAction::Download(item),
        );
    }

    fn initiate_save_download(&mut self, ctx: &egui::Context, item: GopherItem) {
        let suggested = item.file_name_with_forced_ext();
        if let Some(path) = rfd::FileDialog::new()
            .set_title("Download and save file")
            .set_file_name(&suggested)
            .save_file()
        {
            let repaint_ctx = ctx.clone();
            self.downloads
                .start_download(item, path, false, move || repaint_ctx.request_repaint());
            self.download_window.show_window();
        }
    }

    fn save_current_page(&mut self) {
        let Some(page) = self.page_view.current_page() else {
            return;
        };
        let suggested = page.file_name();
        if let Some(path) = rfd::FileDialog::new()
            .set_title("Save current file")
            .set_file_name(&suggested)
            .save_file()
        {
            if let Err(err) = page.save_as_file(&path) {
                self.message_view
                    .show_info(format!("Failed to save file: {}", err));
            }
        }
    }

    fn apply_loaded_page(&mut self, ctx: &egui::Context, page: GopherPage, record_history: bool) {
        let title = format!(
            "{} ({}) - {}",
            page.url().to_url_string(false),
            SystemUtility::format_file_size(page.raw_bytes().len() as u64),
            APPLICATION_TITLE
        );
        ctx.send_viewport_cmd(ViewportCommand::Title(title));

        let address = self.format_display_address(page.url(), page.content_type());
        self.navigation_bar.set_address_text(address);
        self.page_view.set_page(ctx, page.clone());
        self.navigation_bar.set_is_loading(false);

        if record_history {
            self.push_history(page);
        }
        self.sync_history_buttons();
    }

    fn push_history(&mut self, page: GopherPage) {
        let new_url = page.url().to_url_string(false);

        match self.history_pos {
            Some(pos) => {
                if self.history[pos].url().to_url_string(false) == new_url {
                    // Reload of the current page: update cached entry in place.
                    self.history[pos] = page;
                    return;
                }
                self.history.truncate(pos + 1);
                self.history.push(page);
                self.history_pos = Some(self.history.len() - 1);
            }
            None => {
                self.history.push(page);
                self.history_pos = Some(0);
            }
        }
    }

    fn sync_history_buttons(&mut self) {
        let (can_back, can_forward) = match self.history_pos {
            Some(pos) => (pos > 0, pos + 1 < self.history.len()),
            None => (false, false),
        };
        self.navigation_bar.set_can_go_back(can_back);
        self.navigation_bar.set_can_go_forward(can_forward);
    }

    fn navigate_back(&mut self, ctx: &egui::Context) {
        if let Some(pos) = self.history_pos {
            if pos > 0 {
                let target = pos - 1;
                self.history_pos = Some(target);
                let page = self.history[target].clone();
                self.apply_loaded_page(ctx, page, false);
            }
        }
    }

    fn navigate_forward(&mut self, ctx: &egui::Context) {
        if let Some(pos) = self.history_pos {
            if pos + 1 < self.history.len() {
                let target = pos + 1;
                self.history_pos = Some(target);
                let page = self.history[target].clone();
                self.apply_loaded_page(ctx, page, false);
            }
        }
    }

    fn reload_current(&mut self, ctx: &egui::Context) {
        if let Some(pos) = self.history_pos {
            let page = &self.history[pos];
            let url = page.url().to_url_string(false);
            let content_type = page.content_type();
            self.fetch_gopher_content(ctx, &url, content_type);
        }
    }

    fn navigate_home(&mut self, ctx: &egui::Context) {
        let home = self.config.gopher_home();
        self.fetch_gopher_content(ctx, &home, GopherItemType::GopherMenu);
    }

    fn ensure_caps_fetched(&mut self, ctx: &egui::Context, host: &str, port: u16) {
        if self.caps_cache.needs_fetch(host, port) {
            self.caps_cache.mark_in_flight(host, port);
            let repaint_ctx = ctx.clone();
            GopherClient::fetch_caps_async(
                host.to_string(),
                port,
                self.caps_tx.clone(),
                move || repaint_ctx.request_repaint(),
            );
        }
    }

    fn poll_caps_events(&mut self) {
        while let Ok(event) = self.caps_rx.try_recv() {
            self.caps_cache.insert(&event.host, event.port, event.caps);
        }
    }

    fn current_host_caps(&self) -> Option<(String, u16, &GopherCaps)> {
        let page = self.page_view.current_page()?;
        let host = page.url().host();
        let port = page.url().port();
        let caps = self.caps_cache.get(host, port)?;
        Some((host.to_string(), port, caps))
    }

    fn poll_network_events(&mut self, ctx: &egui::Context) {
        while let Ok(event) = self.fetch_rx.try_recv() {
            match event {
                FetchEvent::Progress {
                    request_id,
                    url,
                    bytes_loaded,
                } if request_id == self.active_request_id => {
                    let title = format!(
                        "{} ({}) - {}",
                        url.to_url_string(false),
                        SystemUtility::format_file_size(bytes_loaded),
                        APPLICATION_TITLE
                    );
                    ctx.send_viewport_cmd(ViewportCommand::Title(title));
                }
                FetchEvent::Loaded { request_id, page }
                    if request_id == self.active_request_id =>
                {
                    self.apply_loaded_page(ctx, page, true);
                }
                FetchEvent::Failed {
                    request_id,
                    error,
                    url,
                } if request_id == self.active_request_id => {
                    self.navigation_bar.set_is_loading(false);
                    let msg = match error {
                        GopherError::ConnectFailed => {
                            format!("Connection refused: {}", url.host())
                        }
                        GopherError::ConnectionTimeout => {
                            format!("Connection timed out: {}", url.host())
                        }
                        GopherError::HostUnknown => format!("Server not found: {}", url.host()),
                        GopherError::InvalidUrl => String::from("Invalid Gopher address."),
                        GopherError::UserCancelled => String::new(),
                        GopherError::ParserFailed => String::from("Failed to parse Gopher page."),
                        GopherError::Exception(detail) => {
                            format!("An error occurred: {}", detail)
                        }
                    };
                    if !msg.is_empty() {
                        self.message_view.show_info(msg);
                    }
                }
                FetchEvent::ItemMismatch {
                    request_id,
                    detected,
                    url,
                    ..
                } if request_id == self.active_request_id => {
                    self.navigation_bar.set_is_loading(false);
                    self.confirm_download(GopherItem::from_url(detected, &url));
                }
                _ => {}
            }
        }
    }

    fn handle_shortcuts(&mut self, ctx: &egui::Context) {
        let mut focus_url = false;
        let mut reload = false;
        let mut go_back = false;
        let mut go_forward = false;
        let mut select_all = false;
        let mut copy_selection = false;
        let mut escape = false;

        ctx.input(|i| {
            let cmd = i.modifiers.command;
            if cmd && i.key_pressed(Key::L) {
                focus_url = true;
            }
            if (cmd && i.key_pressed(Key::R)) || i.key_pressed(Key::F5) {
                reload = true;
            }
            if (i.modifiers.alt && i.key_pressed(Key::ArrowLeft))
                || (cmd && i.key_pressed(Key::OpenBracket))
            {
                go_back = true;
            }
            if (i.modifiers.alt && i.key_pressed(Key::ArrowRight))
                || (cmd && i.key_pressed(Key::CloseBracket))
            {
                go_forward = true;
            }
            if cmd && i.key_pressed(Key::A) && !i.focused {
                select_all = true;
            }
            if i.modifiers == Modifiers::COMMAND && i.key_pressed(Key::A) {
                select_all = true;
            }
            if cmd && i.key_pressed(Key::C) {
                copy_selection = true;
            }
            if i.key_pressed(Key::Escape) {
                escape = true;
            }
        });

        if focus_url {
            self.navigation_bar.request_address_focus();
        }
        if reload {
            self.reload_current(ctx);
        }
        if go_back {
            self.navigate_back(ctx);
        }
        if go_forward {
            self.navigate_forward(ctx);
        }
        if select_all && !ctx.wants_keyboard_input() {
            self.page_view.select_all(ctx);
        }
        if copy_selection && !ctx.wants_keyboard_input() {
            let selected = self.page_view.selected_text();
            if !selected.is_empty() {
                PageMenu::copy_text(&selected);
            }
        }
        if escape {
            if self.navigation_bar.is_loading() {
                self.client.cancel_fetch();
                self.navigation_bar.set_is_loading(false);
            } else if self.search_input.is_visible() {
                self.search_input.hide();
            } else if self.message_view.is_visible() {
                self.message_view.hide();
            } else {
                self.page_view.clear_selection(ctx);
            }
        }
    }
}

impl eframe::App for MainWindow {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        if !self.initial_load_started {
            self.initial_load_started = true;
            self.navigate_home(ctx);
        }

        self.poll_network_events(ctx);
        self.poll_caps_events();
        self.downloads.poll_events();
        self.handle_shortcuts(ctx);

        let theme = self.config.theme().clone();
        if self.applied_window_appearance != Some(theme.window_appearance) {
            ctx.set_theme(theme.window_appearance.to_theme_preference());
            ctx.send_viewport_cmd(ViewportCommand::SetTheme(
                theme.window_appearance.to_system_theme(),
            ));
            self.applied_window_appearance = Some(theme.window_appearance);
        }
        let prefix_enabled = self.config.selector_prefix_enabled();

        // Top header bar: MessageView + SearchInput
        if self.message_view.is_visible() || self.search_input.is_visible() {
            egui::TopBottomPanel::top("gophie_header_bar")
                .frame(egui::Frame::NONE)
                .show_separator_line(false)
                .show(ctx, |ui| {
                    ui.spacing_mut().item_spacing.y = 0.0;

                    if let Some(outcome) = self.message_view.show(ui, &theme) {
                        match outcome {
                            MessageOutcome::DownloadAndOpen(item) => {
                                let target = self.config.download_dir().join(item.file_name());
                                let repaint_ctx = ctx.clone();
                                self.downloads.start_download(item, target, true, move || {
                                    repaint_ctx.request_repaint();
                                });
                                self.download_window.show_window();
                            }
                            MessageOutcome::DownloadSaveAs(item) => {
                                self.initiate_save_download(ctx, item);
                            }
                            MessageOutcome::OpenWeb(url) => {
                                if let Err(err) = SystemUtility::open_uri(&url) {
                                    self.message_view.show_info(err);
                                }
                            }
                            MessageOutcome::OpenEmail(email) => {
                                let uri = format!("mailto:{}", email);
                                if let Err(err) = SystemUtility::open_uri(&uri) {
                                    self.message_view.show_info(err);
                                }
                            }
                            MessageOutcome::OpenTelnet { host, port } => {
                                let uri = format!("telnet://{}:{}", host, port);
                                if let Err(err) = SystemUtility::open_uri(&uri) {
                                    self.message_view.show_info(err);
                                }
                            }
                        }
                    }

                    if let Some(submission) = self.search_input.show(ui, &theme) {
                        let query_url = format!("{}\t{}", submission.target_url, submission.query);
                        self.fetch_gopher_content(ctx, &query_url, GopherItemType::GopherMenu);
                    }
                });
        }

        let has_server_caps = self.current_host_caps().is_some();

        // Bottom navigation bar
        egui::TopBottomPanel::bottom("gophie_nav_bar")
            .frame(egui::Frame::NONE)
            .show_separator_line(false)
            .show(ctx, |ui| {
                if let Some(nav_action) = self.navigation_bar.show(
                    ui,
                    &theme,
                    prefix_enabled,
                    self.downloads.active_count(),
                    has_server_caps,
                ) {
                    match nav_action {
                        NavAction::Back => self.navigate_back(ctx),
                        NavAction::Forward => self.navigate_forward(ctx),
                        NavAction::Refresh => self.reload_current(ctx),
                        NavAction::Stop => {
                            self.client.cancel_fetch();
                            self.navigation_bar.set_is_loading(false);
                        }
                        NavAction::Home => self.navigate_home(ctx),
                        NavAction::Search => {
                            if self.search_input.is_visible() {
                                self.search_input.hide();
                            } else {
                                let engine = self.config.search_engine();
                                self.search_input.open("Search Gopherspace", engine);
                            }
                        }
                        NavAction::Navigate { address, item } => {
                            self.handle_address_request(ctx, &address, item);
                        }
                        NavAction::ToggleDownloads => self.download_window.toggle(),
                        NavAction::ToggleSettings => {
                            self.settings_window.toggle(self.config.settings());
                        }
                        NavAction::ToggleServerInfo => {
                            self.server_info_window.toggle();
                        }
                    }
                }
            });

        // Optional caps.txt breadcrumb strip when inside a subdirectory
        let breadcrumb_target = self.render_breadcrumb_bar(ctx, &theme);
        if let Some(target_url) = breadcrumb_target {
            self.fetch_gopher_content(ctx, &target_url, GopherItemType::GopherMenu);
        }

        // Main page view
        egui::CentralPanel::default()
            .frame(egui::Frame::NONE)
            .show(ctx, |ui| {
                if let Some(view_action) = self.page_view.show(ui, &theme, prefix_enabled) {
                    match view_action {
                        PageViewAction::LinkClicked(item) => {
                            let url = item.url_string();
                            self.handle_address_request(ctx, &url, item);
                        }
                        PageViewAction::Menu(menu_action) => match menu_action {
                            PageMenuAction::SaveCurrentPage => self.save_current_page(),
                            PageMenuAction::SaveLinkAs(item) => {
                                self.initiate_save_download(ctx, item);
                            }
                            PageMenuAction::SelectAll => self.page_view.select_all(ctx),
                            PageMenuAction::SetAsHomeGopher(url) => {
                                self.config.set_gopher_home(&url);
                                self.message_view
                                    .show_info(format!("Home Gopher set to \"{}\".", url));
                            }
                        },
                    }
                }
            });

        // Secondary native Downloads window
        if let Some(dl_action) = self.download_window.show(ctx, &self.downloads, &theme) {
            match dl_action {
                DownloadWindowAction::Abort(id) => self.downloads.abort_and_remove(id),
                DownloadWindowAction::Retry(id) => {
                    let repaint_ctx = ctx.clone();
                    self.downloads
                        .retry(id, move || repaint_ctx.request_repaint());
                }
                DownloadWindowAction::Open(id) => {
                    if let Some(item) = self.downloads.get(id) {
                        item.open_on_desktop();
                    }
                }
                DownloadWindowAction::ClearNonActive => self.downloads.clear_non_active(),
            }
        }

        // Secondary native Settings window
        if let Some(updated_settings) = self.settings_window.show(ctx, &theme) {
            if let Err(err) = self.config.apply_settings(updated_settings) {
                self.message_view
                    .show_info(format!("Failed to save settings: {}", err));
            } else if let Some(page) = self.page_view.current_page() {
                let addr = self.format_display_address(page.url(), page.content_type());
                self.navigation_bar.set_address_text(addr);
            }
        }
        // Secondary native Server Info (caps.txt) window
        let info_action = if let Some((host, _port, caps)) = self.current_host_caps() {
            let caps_cloned = caps.clone();
            self.server_info_window
                .show(ctx, &host, Some(&caps_cloned), &theme)
        } else {
            self.server_info_window.show(ctx, "", None, &theme)
        };
        if let Some(ServerInfoAction::EmailAdmin(email)) = info_action {
            self.message_view.show_confirm(
                format!("Do you want to send an e-mail to \"{}\"?", email),
                &["Create new e-mail", "Dismiss"],
                ConfirmAction::OpenEmail(email),
            );
        }
    }
}

impl MainWindow {
    /// Renders a clickable `caps.txt` breadcrumb trail above the page view when inside a sub-selector.
    fn render_breadcrumb_bar(
        &self,
        ctx: &egui::Context,
        theme: &crate::config::Theme,
    ) -> Option<String> {
        let page = self.page_view.current_page()?;
        let host = page.url().host();
        let port = page.url().port();
        let caps = self.caps_cache.get(host, port)?;
        let crumbs = caps.breadcrumbs(page.url().wire_selector());
        if crumbs.is_empty() {
            return None;
        }

        let delim = caps.path_delimiter.as_deref().unwrap_or("/");
        let mut clicked_url = None;

        let strip_bg = if theme.view_background.r() > 128 {
            egui::Color32::from_rgb(0xEC, 0xEC, 0xEC)
        } else {
            egui::Color32::from_rgb(0x24, 0x24, 0x24)
        };

        egui::TopBottomPanel::top("gophie_breadcrumbs")
            .frame(
                egui::Frame::new()
                    .fill(strip_bg)
                    .inner_margin(egui::Margin::symmetric(14, 5)),
            )
            .show_separator_line(false)
            .show(ctx, |ui| {
                ui.horizontal_wrapped(|ui| {
                    ui.spacing_mut().item_spacing.x = 6.0;
                    let muted = theme.view_text.gamma_multiply(0.55);

                    let root_resp = ui
                        .add(
                            egui::Label::new(
                                egui::RichText::new(host)
                                    .font(egui::FontId::monospace(13.0))
                                    .color(theme.page_link),
                            )
                            .selectable(false)
                            .sense(egui::Sense::click()),
                        )
                        .on_hover_cursor(egui::CursorIcon::PointingHand);
                    if root_resp.clicked() {
                        clicked_url = Some(GopherUrl::from_parts(host, port, "").to_url_string(false));
                    }

                    for (idx, crumb) in crumbs.iter().enumerate() {
                        ui.add(
                            egui::Label::new(
                                egui::RichText::new(delim)
                                    .font(egui::FontId::monospace(13.0))
                                    .color(muted),
                            )
                            .selectable(false),
                        );

                        let is_last = idx + 1 == crumbs.len();
                        let color = if is_last {
                            theme.view_text
                        } else {
                            theme.page_link
                        };
                        let sense = if is_last {
                            egui::Sense::hover()
                        } else {
                            egui::Sense::click()
                        };

                        let resp = ui.add(
                            egui::Label::new(
                                egui::RichText::new(&crumb.label)
                                    .font(egui::FontId::monospace(13.0))
                                    .color(color),
                            )
                            .selectable(false)
                            .sense(sense),
                        );
                        if !is_last
                            && resp
                                .on_hover_cursor(egui::CursorIcon::PointingHand)
                                .clicked()
                        {
                            clicked_url = Some(
                                GopherUrl::from_parts(host, port, &crumb.selector)
                                    .to_url_string(false),
                            );
                        }
                    }
                });
            });

        clicked_url
    }
}
