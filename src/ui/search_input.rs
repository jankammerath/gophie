//! Top search input bar for Gopher Type-7 full-text search queries.

use eframe::egui::{self, FontId, Frame, Key, Margin, RichText, TextEdit};

use crate::config::Theme;

use super::assets::{feather_font, icons};

/// Submitted Type-7 Gopher search request.
#[derive(Debug, Clone)]
pub struct SearchSubmission {
    pub target_url: String,
    pub query: String,
}

/// Slide-down bar shown when the user activates a Type-7 Gopher search item.
#[derive(Debug, Default)]
pub struct SearchInput {
    visible: bool,
    title: String,
    target_url: String,
    query: String,
    request_focus: bool,
}

impl SearchInput {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn is_visible(&self) -> bool {
        self.visible
    }

    pub fn hide(&mut self) {
        self.visible = false;
        self.request_focus = false;
    }

    /// Opens the search bar for `target_url` with `title` as the prompt label.
    pub fn open(&mut self, title: impl Into<String>, target_url: impl Into<String>) {
        self.title = title.into();
        self.target_url = target_url.into();
        self.query.clear();
        self.visible = true;
        self.request_focus = true;
    }

    /// Renders the search bar if visible and returns a `SearchSubmission` on Enter.
    pub fn show(&mut self, ui: &mut egui::Ui, theme: &Theme) -> Option<SearchSubmission> {
        if !self.visible {
            return None;
        }

        let mut submission = None;
        let mut close = false;

        Frame::new()
            .fill(theme.search_background)
            .inner_margin(Margin::symmetric(12, 8))
            .show(ui, |ui| {
                ui.set_width(ui.available_width());
                ui.horizontal(|ui| {
                    ui.label(
                        RichText::new(icons::SEARCH)
                            .font(feather_font(16.0))
                            .color(theme.search_title),
                    );
                    ui.add_space(4.0);
                    ui.label(
                        RichText::new(&self.title)
                            .font(FontId::proportional(14.0))
                            .color(theme.search_title),
                    );
                    ui.add_space(8.0);

                    let edit = TextEdit::singleline(&mut self.query)
                        .font(FontId::proportional(14.0))
                        .text_color(theme.search_text)
                        .frame(false)
                        .hint_text(
                            RichText::new("Type search query and press Enter…")
                                .color(theme.search_title.gamma_multiply(0.8)),
                        )
                        .desired_width(ui.available_width());

                    let response = ui.add(edit);
                    if self.request_focus {
                        response.request_focus();
                        self.request_focus = false;
                    }

                    if response.lost_focus() && ui.input(|i| i.key_pressed(Key::Enter)) {
                        let trimmed = self.query.trim().to_string();
                        if !trimmed.is_empty() {
                            submission = Some(SearchSubmission {
                                target_url: self.target_url.clone(),
                                query: trimmed,
                            });
                        }
                        close = true;
                    }

                    if ui.input(|i| i.key_pressed(Key::Escape)) {
                        close = true;
                    }
                });
            });

        if close {
            self.hide();
        }
        submission
    }
}
