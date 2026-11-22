//! Bottom navigation bar with history controls, address input, download tray, and spinner.

use eframe::egui::{self, CursorIcon, FontId, Frame, Key, Margin, Sense, Spinner, TextEdit};

use crate::config::Theme;
use crate::net::{GopherItem, GopherUrl};

use super::assets::{feather_font, icons};

/// Actions emitted by `NavigationBar` to `MainWindow`.
#[derive(Debug, Clone)]
pub enum NavAction {
    Back,
    Forward,
    Refresh,
    Stop,
    Home,
    Navigate { address: String, item: GopherItem },
    ToggleDownloads,
    ToggleSettings,
}

/// Bottom navigation bar matching Gophie's blue toolbar layout.
pub struct NavigationBar {
    address_text: String,
    can_go_back: bool,
    can_go_forward: bool,
    is_loading: bool,
    focus_address: bool,
}

impl NavigationBar {
    pub fn new(initial_address: impl Into<String>) -> Self {
        Self {
            address_text: initial_address.into(),
            can_go_back: false,
            can_go_forward: false,
            is_loading: false,
            focus_address: false,
        }
    }

    pub fn set_address_text(&mut self, text: impl Into<String>) {
        self.address_text = text.into();
    }

    pub fn set_can_go_back(&mut self, allowed: bool) {
        self.can_go_back = allowed;
    }

    pub fn set_can_go_forward(&mut self, allowed: bool) {
        self.can_go_forward = allowed;
    }

    pub fn set_is_loading(&mut self, loading: bool) {
        self.is_loading = loading;
    }

    pub fn is_loading(&self) -> bool {
        self.is_loading
    }

    pub fn request_address_focus(&mut self) {
        self.focus_address = true;
    }

    /// Renders the navigation bar and returns any triggered `NavAction`.
    pub fn show(
        &mut self,
        ui: &mut egui::Ui,
        theme: &Theme,
        selector_prefix_enabled: bool,
        active_downloads: usize,
    ) -> Option<NavAction> {
        let mut action = None;

        Frame::new()
            .fill(theme.nav_background)
            .inner_margin(Margin::symmetric(8, 6))
            .show(ui, |ui| {
                ui.set_width(ui.available_width());
                ui.horizontal(|ui| {
                    ui.spacing_mut().item_spacing.x = 12.0;

                    if Self::icon_button(ui, icons::BACK, self.can_go_back, "Back", theme) {
                        action = Some(NavAction::Back);
                    }
                    if Self::icon_button(ui, icons::FORWARD, self.can_go_forward, "Forward", theme) {
                        action = Some(NavAction::Forward);
                    }

                    let (reload_icon, reload_tip) = if self.is_loading {
                        (icons::STOP, "Stop")
                    } else {
                        (icons::REFRESH, "Reload")
                    };
                    if Self::icon_button(ui, reload_icon, true, reload_tip, theme) {
                        action = Some(if self.is_loading {
                            NavAction::Stop
                        } else {
                            NavAction::Refresh
                        });
                    }

                    if Self::icon_button(ui, icons::HOME, true, "Home Gopher", theme) {
                        action = Some(NavAction::Home);
                    }

                    // Reserve space on the right for downloads, settings, and optional loading spinner.
                    let right_reserved = if self.is_loading { 96.0 } else { 68.0 };
                    let input_width = (ui.available_width() - right_reserved).max(120.0);

                    ui.scope(|ui| {
                        ui.visuals_mut().selection.bg_fill = theme.nav_selection;
                        ui.visuals_mut().selection.stroke.color = theme.nav_text_hover;

                        let response = ui.add(
                            TextEdit::singleline(&mut self.address_text)
                                .font(FontId::proportional(14.0))
                                .text_color(theme.nav_text_hover)
                                .frame(false)
                                .desired_width(input_width),
                        );

                        if self.focus_address {
                            response.request_focus();
                            self.focus_address = false;
                        }

                        if response.lost_focus() && ui.input(|i| i.key_pressed(Key::Enter)) {
                            let requested = self.address_text.trim().to_string();
                            if !requested.is_empty() {
                                let mut address = requested.clone();
                                let mut item = GopherItem::default();

                                if selector_prefix_enabled {
                                    let parsed = GopherUrl::parse(&requested);
                                    if let Some(prefix) = parsed.type_prefix() {
                                        item = GopherItem::from_code_and_url(prefix, &parsed);
                                        address = parsed.to_url_string(false);
                                    }
                                }

                                action = Some(NavAction::Navigate { address, item });
                            }
                        }
                    });

                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        if Self::icon_button(ui, icons::SETTINGS, true, "Settings", theme) {
                            action = Some(NavAction::ToggleSettings);
                        }

                        let dl_tip = if active_downloads > 0 {
                            format!("Downloads ({} active)", active_downloads)
                        } else {
                            "Downloads".to_string()
                        };
                        if Self::icon_button(ui, icons::DOWNLOADS, true, &dl_tip, theme) {
                            action = Some(NavAction::ToggleDownloads);
                        }

                        if self.is_loading {
                            ui.add(Spinner::new().size(16.0).color(theme.nav_text_hover));
                        }
                    });
                });
            });

        action
    }

    fn icon_button(
        ui: &mut egui::Ui,
        glyph: &str,
        enabled: bool,
        tooltip: &str,
        theme: &Theme,
    ) -> bool {
        let sense = if enabled {
            Sense::click()
        } else {
            Sense::hover()
        };

        let (rect, response) = ui.allocate_exact_size(egui::vec2(22.0, 22.0), sense);
        let hovered = enabled && response.hovered();
        let color = if !enabled {
            theme.nav_text.gamma_multiply(0.45)
        } else if hovered {
            theme.nav_text_hover
        } else {
            theme.nav_text
        };

        ui.painter().text(
            rect.center(),
            egui::Align2::CENTER_CENTER,
            glyph,
            feather_font(18.0),
            color,
        );

        let response = if enabled {
            response
                .on_hover_cursor(CursorIcon::PointingHand)
                .on_hover_text(tooltip)
        } else {
            response
        };

        enabled && response.clicked()
    }
}
