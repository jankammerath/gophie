//! Top notification and confirmation banner (yellow bar in Gophie).

use eframe::egui::{self, Button, CornerRadius, FontId, Frame, Margin, RichText, Stroke};

use crate::config::Theme;
use crate::net::GopherItem;

use super::assets::{feather_font, icons};

/// Action pending user confirmation in `MessageView`.
#[derive(Debug, Clone)]
pub enum ConfirmAction {
    Download(GopherItem),
    OpenWeb(String),
    OpenEmail(String),
    OpenTelnet { host: String, port: u16 },
}

/// Outcome returned when the user clicks a button in `MessageView`.
#[derive(Debug, Clone)]
pub enum MessageOutcome {
    DownloadAndOpen(GopherItem),
    DownloadSaveAs(GopherItem),
    OpenWeb(String),
    OpenEmail(String),
    OpenTelnet { host: String, port: u16 },
}

#[derive(Debug, Clone)]
enum MessageMode {
    Info,
    Confirm {
        options: Vec<String>,
        action: ConfirmAction,
    },
}

/// Displays non-modal informational alerts and confirmation prompts above the page view.
#[derive(Debug, Default)]
pub struct MessageView {
    visible: bool,
    text: String,
    mode: Option<MessageMode>,
}

impl MessageView {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn is_visible(&self) -> bool {
        self.visible
    }

    pub fn hide(&mut self) {
        self.visible = false;
        self.mode = None;
    }

    /// Shows an informational banner with a single `Close` button.
    pub fn show_info(&mut self, text: impl Into<String>) {
        self.text = text.into();
        self.mode = Some(MessageMode::Info);
        self.visible = true;
    }

    /// Shows a confirmation banner with custom option buttons.
    pub fn show_confirm(
        &mut self,
        text: impl Into<String>,
        options: &[&str],
        action: ConfirmAction,
    ) {
        self.text = text.into();
        self.mode = Some(MessageMode::Confirm {
            options: options.iter().map(|s| (*s).to_string()).collect(),
            action,
        });
        self.visible = true;
    }

    /// Renders the banner if visible and returns any user-confirmed action.
    pub fn show(&mut self, ui: &mut egui::Ui, theme: &Theme) -> Option<MessageOutcome> {
        if !self.visible {
            return None;
        }

        let mode = self.mode.clone()?;

        let mut outcome = None;
        let mut dismiss = false;

        Frame::new()
            .fill(theme.message_background)
            .inner_margin(Margin::symmetric(14, 10))
            .show(ui, |ui| {
                ui.set_width(ui.available_width());
                ui.horizontal(|ui| {
                    let icon = match &mode {
                        MessageMode::Info => icons::INFO,
                        MessageMode::Confirm { .. } => icons::CONFIRM,
                    };
                    ui.label(
                        RichText::new(icon)
                            .font(feather_font(18.0))
                            .color(theme.message_foreground),
                    );
                    ui.add_space(6.0);
                    ui.label(
                        RichText::new(&self.text)
                            .font(FontId::proportional(13.0))
                            .color(theme.message_foreground),
                    );

                    ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                        let make_btn = |label: &str| {
                            Button::new(
                                RichText::new(label)
                                    .font(FontId::proportional(12.0))
                                    .color(theme.message_foreground),
                            )
                            .fill(egui::Color32::TRANSPARENT)
                            .stroke(Stroke::new(1.0_f32, theme.message_foreground))
                            .corner_radius(CornerRadius::same(6))
                        };

                        match &mode {
                            MessageMode::Info => {
                                if ui.add(make_btn("Close")).clicked() {
                                    dismiss = true;
                                }
                            }
                            MessageMode::Confirm { options, action } => {
                                // Render right-to-left so option 0 appears leftmost.
                                for (idx, label) in options.iter().enumerate().rev() {
                                    if ui.add(make_btn(label)).clicked() {
                                        outcome = Self::resolve_option(action, idx);
                                        dismiss = true;
                                    }
                                    ui.add_space(4.0);
                                }
                            }
                        }
                    });
                });
            });

        if dismiss {
            self.hide();
        }
        outcome
    }

    fn resolve_option(action: &ConfirmAction, option_idx: usize) -> Option<MessageOutcome> {
        match (action, option_idx) {
            (ConfirmAction::Download(item), 0) => Some(MessageOutcome::DownloadAndOpen(item.clone())),
            (ConfirmAction::Download(item), 1) => Some(MessageOutcome::DownloadSaveAs(item.clone())),
            (ConfirmAction::OpenWeb(url), 0) => Some(MessageOutcome::OpenWeb(url.clone())),
            (ConfirmAction::OpenEmail(addr), 0) => Some(MessageOutcome::OpenEmail(addr.clone())),
            (ConfirmAction::OpenTelnet { host, port }, 0) => Some(MessageOutcome::OpenTelnet {
                host: host.clone(),
                port: *port,
            }),
            _ => None,
        }
    }
}
