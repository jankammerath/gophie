//! Native Server Info dialog displaying parsed `caps.txt` metadata for the current Gopher host.

use eframe::egui::{
    self, Button, Color32, CornerRadius, CursorIcon, FontId, Frame, Grid, Key, Margin, RichText,
    ScrollArea, Stroke, Vec2, ViewportBuilder, ViewportCommand, ViewportId,
};

use crate::config::{Theme, WindowAppearance};
use crate::net::GopherCaps;

/// Actions emitted by `ServerInfoWindow`.
#[derive(Debug, Clone)]
pub enum ServerInfoAction {
    EmailAdmin(String),
}

/// Manages visibility of the native Server Info (`caps.txt`) window.
#[derive(Debug, Default)]
pub struct ServerInfoWindow {
    visible: bool,
    applied_appearance: Option<WindowAppearance>,
}

impl ServerInfoWindow {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn toggle(&mut self) {
        self.visible = !self.visible;
        if !self.visible {
            self.applied_appearance = None;
        }
    }

    /// Renders the Server Info window if visible and `caps` is available for `host`.
    pub fn show(
        &mut self,
        ctx: &egui::Context,
        host: &str,
        caps: Option<&GopherCaps>,
        theme: &Theme,
    ) -> Option<ServerInfoAction> {
        if !self.visible {
            return None;
        }

        let Some(caps) = caps else {
            self.visible = false;
            self.applied_appearance = None;
            return None;
        };

        let mut action = None;
        let mut close_requested = false;
        let viewport_id = ViewportId::from_hash_of("gophie_server_info_window");
        let builder = ViewportBuilder::default()
            .with_title(format!("Server Info — {}", host))
            .with_inner_size([460.0, 360.0])
            .with_min_inner_size([380.0, 240.0]);

        ctx.show_viewport_immediate(viewport_id, builder, |ctx, _class| {
            if self.applied_appearance != Some(theme.window_appearance) {
                ctx.send_viewport_cmd(ViewportCommand::SetTheme(
                    theme.window_appearance.to_system_theme(),
                ));
                self.applied_appearance = Some(theme.window_appearance);
            }

            if ctx.input(|i| i.viewport().close_requested() || i.key_pressed(Key::Escape)) {
                close_requested = true;
            }

            // Bottom close bar
            egui::TopBottomPanel::bottom("server_info_bar")
                .frame(
                    Frame::new()
                        .fill(theme.actionbar_background)
                        .inner_margin(Margin::symmetric(16, 10)),
                )
                .show(ctx, |ui| {
                    ui.horizontal(|ui| {
                        let version_label = match caps.caps_version {
                            Some(v) => format!("caps.txt v{} · TTL {}s", v, caps.expire_after_secs),
                            None => format!("caps.txt · TTL {}s", caps.expire_after_secs),
                        };
                        ui.label(
                            RichText::new(version_label)
                                .font(FontId::proportional(12.0))
                                .color(theme.actionbar_inactive_text),
                        );

                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.spacing_mut().button_padding = Vec2::new(14.0, 6.0);
                            let close_btn = Button::new(
                                RichText::new("Close")
                                    .font(FontId::proportional(13.0))
                                    .color(theme.actionbar_text),
                            )
                            .fill(Color32::from_white_alpha(24))
                            .stroke(Stroke::new(1.0_f32, theme.actionbar_text))
                            .corner_radius(CornerRadius::same(6))
                            .min_size(Vec2::new(76.0, 28.0));

                            if ui
                                .add(close_btn)
                                .on_hover_cursor(CursorIcon::PointingHand)
                                .clicked()
                            {
                                close_requested = true;
                            }
                        });
                    });
                });

            // Scrollable metadata body
            egui::CentralPanel::default()
                .frame(
                    Frame::new()
                        .fill(theme.view_background)
                        .inner_margin(Margin::same(16)),
                )
                .show(ctx, |ui| {
                    ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();
                    ui.visuals_mut().extreme_bg_color = theme.view_background;

                    let text_color = theme.view_text;
                    let muted = text_color.gamma_multiply(0.65);

                    ScrollArea::vertical()
                        .auto_shrink([false, false])
                        .show(ui, |ui| {
                            ui.label(
                                RichText::new(host)
                                    .font(FontId::proportional(16.0))
                                    .strong()
                                    .color(theme.page_link),
                            );
                            ui.add_space(12.0);

                            let rows = caps.info_rows();
                            if rows.is_empty() {
                                ui.label(
                                    RichText::new("No additional server metadata provided.")
                                        .font(FontId::proportional(13.0))
                                        .color(muted),
                                );
                            } else {
                                Grid::new("server_info_grid")
                                    .num_columns(2)
                                    .min_col_width(110.0)
                                    .spacing([14.0, 10.0])
                                    .show(ui, |ui| {
                                        for (label, value) in rows {
                                            ui.label(
                                                RichText::new(&label)
                                                    .font(FontId::proportional(13.0))
                                                    .strong()
                                                    .color(muted),
                                            );

                                            if label == "Admin" && value.contains('@') {
                                                let resp = ui
                                                    .add(
                                                        egui::Label::new(
                                                            RichText::new(&value)
                                                                .font(FontId::proportional(13.0))
                                                                .color(theme.page_link),
                                                        )
                                                        .sense(egui::Sense::click()),
                                                    )
                                                    .on_hover_cursor(CursorIcon::PointingHand);
                                                if resp.clicked() {
                                                    action =
                                                        Some(ServerInfoAction::EmailAdmin(value));
                                                }
                                            } else {
                                                ui.add(
                                                    egui::Label::new(
                                                        RichText::new(value)
                                                            .font(FontId::proportional(13.0))
                                                            .color(text_color),
                                                    )
                                                    .wrap(),
                                                );
                                            }
                                            ui.end_row();
                                        }
                                    });
                            }
                        });
                });
        });

        if close_requested {
            self.visible = false;
            self.applied_appearance = None;
        }
        action
    }
}
