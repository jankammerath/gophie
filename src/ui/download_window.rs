//! Native secondary Downloads window matching Gophie's dark list and blue action bar.

use eframe::egui::{
    self, CursorIcon, FontId, Frame, Margin, RichText, ScrollArea, Sense, Vec2, ViewportBuilder,
    ViewportId,
};

use crate::config::Theme;
use crate::net::{DownloadManager, DownloadStatus};

use super::assets::{feather_font, icons};

/// Actions triggered by buttons in the Downloads window.
#[derive(Debug, Clone, Copy)]
pub enum DownloadWindowAction {
    Abort(u64),
    Retry(u64),
    Open(u64),
    ClearNonActive,
}

/// Manages visibility and selection state of the native Downloads viewport.
#[derive(Debug, Default)]
pub struct DownloadWindow {
    visible: bool,
    selected_id: Option<u64>,
}

impl DownloadWindow {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn toggle(&mut self) {
        self.visible = !self.visible;
    }

    pub fn show_window(&mut self) {
        self.visible = true;
    }

    /// Renders the Downloads window as a native OS secondary window when visible.
    pub fn show(
        &mut self,
        ctx: &egui::Context,
        downloads: &DownloadManager,
        theme: &Theme,
    ) -> Option<DownloadWindowAction> {
        if !self.visible {
            return None;
        }

        // Clear selection if the previously selected item no longer exists.
        if let Some(sel) = self.selected_id {
            if downloads.get(sel).is_none() {
                self.selected_id = None;
            }
        }

        let mut action = None;
        let mut close_requested = false;
        let viewport_id = ViewportId::from_hash_of("gophie_downloads_window");
        let builder = ViewportBuilder::default()
            .with_title("Downloads")
            .with_inner_size([440.0, 260.0])
            .with_min_inner_size([400.0, 200.0]);

        ctx.show_viewport_immediate(viewport_id, builder, |ctx, _class| {
            if ctx.input(|i| i.viewport().close_requested()) {
                close_requested = true;
            }

            // Bottom blue action bar
            egui::TopBottomPanel::bottom("downloads_action_bar")
                .frame(
                    Frame::new()
                        .fill(theme.actionbar_background)
                        .inner_margin(Margin::symmetric(16, 10)),
                )
                .show(ctx, |ui| {
                    ui.horizontal(|ui| {
                        // Left context-sensitive action button (Abort / Retry / Open / Start)
                        if let Some(selected) = self.selected_id.and_then(|id| downloads.get(id)) {
                            let (icon, label, act) = match selected.status() {
                                DownloadStatus::Active => (
                                    icons::ABORT,
                                    "Abort",
                                    DownloadWindowAction::Abort(selected.id()),
                                ),
                                DownloadStatus::Failed => (
                                    icons::RETRY,
                                    "Retry",
                                    DownloadWindowAction::Retry(selected.id()),
                                ),
                                DownloadStatus::Completed => (
                                    icons::OPEN,
                                    "Open",
                                    DownloadWindowAction::Open(selected.id()),
                                ),
                                DownloadStatus::Idle => (
                                    icons::START,
                                    "Start",
                                    DownloadWindowAction::Retry(selected.id()),
                                ),
                            };

                            if Self::action_button(ui, icon, label, true, theme) {
                                action = Some(act);
                            }
                        }

                        // Right "Clear List" button
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            let can_clear = downloads.has_non_active();
                            if Self::action_button(
                                ui,
                                icons::CLEAR_LIST,
                                "Clear List",
                                can_clear,
                                theme,
                            ) {
                                action = Some(DownloadWindowAction::ClearNonActive);
                            }
                        });
                    });
                });

            // Center dark download list
            egui::CentralPanel::default()
                .frame(Frame::new().fill(theme.filelist_background))
                .show(ctx, |ui| {
                    ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();
                    let visuals = ui.visuals_mut();
                    visuals.extreme_bg_color = theme.filelist_background;
                    visuals.widgets.inactive.bg_stroke = egui::Stroke::NONE;
                    visuals.widgets.hovered.bg_stroke = egui::Stroke::NONE;
                    visuals.widgets.active.bg_stroke = egui::Stroke::NONE;

                    ScrollArea::vertical()
                        .auto_shrink([false, false])
                        .show(ui, |ui| {
                            let mut any_items = false;
                            for item in downloads.items_newest_first() {
                                any_items = true;
                                let is_selected = self.selected_id == Some(item.id());
                                let fill = if is_selected {
                                    theme.download_selected
                                } else {
                                    egui::Color32::TRANSPARENT
                                };

                                let resp = Frame::new()
                                    .fill(fill)
                                    .inner_margin(Margin::symmetric(10, 6))
                                    .show(ui, |ui| {
                                        ui.set_width(ui.available_width());
                                        ui.vertical(|ui| {
                                            ui.label(
                                                RichText::new(item.gopher_item().file_name())
                                                    .font(FontId::monospace(15.0))
                                                    .strong()
                                                    .color(theme.download_title),
                                            );
                                            ui.add_space(2.0);
                                            ui.label(
                                                RichText::new(item.status_summary())
                                                    .font(FontId::monospace(13.0))
                                                    .color(theme.download_text),
                                            );
                                        });
                                    })
                                    .response
                                    .interact(Sense::click());

                                if resp.clicked() {
                                    self.selected_id = Some(item.id());
                                }
                                if resp.double_clicked()
                                    && item.status() == DownloadStatus::Completed
                                {
                                    action = Some(DownloadWindowAction::Open(item.id()));
                                }
                            }

                            if !any_items {
                                ui.centered_and_justified(|ui| {
                                    ui.label(
                                        RichText::new("No downloads yet")
                                            .font(FontId::proportional(13.0))
                                            .color(theme.download_text.gamma_multiply(0.6)),
                                    );
                                });
                            }
                        });
                });
        });

        if close_requested {
            self.visible = false;
        }
        action
    }

    fn action_button(
        ui: &mut egui::Ui,
        icon: &str,
        label: &str,
        enabled: bool,
        theme: &Theme,
    ) -> bool {
        let icon_galley = ui.painter().layout_no_wrap(
            icon.to_string(),
            feather_font(14.0),
            egui::Color32::WHITE,
        );
        let text_galley = ui.painter().layout_no_wrap(
            label.to_string(),
            FontId::proportional(13.0),
            egui::Color32::WHITE,
        );

        let gap = 7.0;
        let pad_x = 8.0;
        let pad_y = 5.0;
        let size = Vec2::new(
            pad_x * 2.0 + icon_galley.size().x + gap + text_galley.size().x,
            (icon_galley.size().y.max(text_galley.size().y) + pad_y * 2.0).max(28.0),
        );

        let sense = if enabled {
            Sense::click()
        } else {
            Sense::hover()
        };
        let (rect, response) = ui.allocate_exact_size(size, sense);

        let hovered = enabled && response.hovered();
        let color = if !enabled {
            theme.actionbar_inactive_text.gamma_multiply(0.5)
        } else if hovered {
            theme.actionbar_text
        } else {
            theme.actionbar_inactive_text
        };

        let icon_pos = egui::pos2(
            rect.left() + pad_x,
            rect.center().y - icon_galley.size().y * 0.5,
        );
        let text_pos = egui::pos2(
            icon_pos.x + icon_galley.size().x + gap,
            rect.center().y - text_galley.size().y * 0.5,
        );

        ui.painter().text(
            icon_pos,
            egui::Align2::LEFT_TOP,
            icon,
            feather_font(14.0),
            color,
        );
        ui.painter().text(
            text_pos,
            egui::Align2::LEFT_TOP,
            label,
            FontId::proportional(13.0),
            color,
        );

        let response = if enabled {
            response.on_hover_cursor(CursorIcon::PointingHand)
        } else {
            response
        };

        enabled && response.clicked()
    }
}
