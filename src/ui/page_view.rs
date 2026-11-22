//! High-performance virtualized Gopher page viewer for menus, plain text, and inline images.

use eframe::egui::{
    self, Align, ColorImage, CursorIcon, FontId, Frame, Layout, Margin, RichText, ScrollArea,
    Sense, TextureHandle, TextureOptions, Vec2,
};

use crate::config::Theme;
use crate::net::{GopherItem, GopherItemType, GopherPage};

use super::assets::{feather_font, icons};
use super::page_menu::{PageMenu, PageMenuAction};

/// User interactions emitted by `PageView`.
#[derive(Debug, Clone)]
pub enum PageViewAction {
    LinkClicked(GopherItem),
    Menu(PageMenuAction),
}

/// Keyboard scroll commands handled by `PageView`.
#[derive(Debug, Clone, Copy)]
pub enum ScrollCommand {
    LineUp,
    LineDown,
    PageUp,
    PageDown,
    Top,
    Bottom,
}

struct RenderContext<'a> {
    theme: &'a Theme,
    selected_all: bool,
    selected_text: &'a str,
    selector_prefix_enabled: bool,
    pending_scroll_y: Option<f32>,
    focused_link_idx: Option<usize>,
    scroll_to_focused: bool,
}

struct ScrollMetrics {
    offset_y: f32,
    viewport_height: f32,
    content_height: f32,
    visible_rows: std::ops::Range<usize>,
}

enum CachedContent {
    Empty,
    Menu,
    Text { lines: Vec<String> },
    Image { texture: TextureHandle, size: Vec2 },
    ImageError(String),
}

/// Renders the active `GopherPage` with virtualized row scrolling and cached image textures.
pub struct PageView {
    current_page: Option<GopherPage>,
    cached: CachedContent,
    selected_all: bool,
    pending_scroll_y: Option<f32>,
    focused_link_idx: Option<usize>,
    scroll_to_focused: bool,
    scroll_offset_y: f32,
    viewport_height: f32,
    content_height: f32,
    visible_rows: std::ops::Range<usize>,
}

impl Default for PageView {
    fn default() -> Self {
        Self::new()
    }
}

impl PageView {
    pub fn new() -> Self {
        Self {
            current_page: None,
            cached: CachedContent::Empty,
            selected_all: false,
            pending_scroll_y: None,
            focused_link_idx: None,
            scroll_to_focused: false,
            scroll_offset_y: 0.0,
            viewport_height: 400.0,
            content_height: 0.0,
            visible_rows: 0..0,
        }
    }

    /// Returns the currently displayed `GopherPage`, if any.
    pub fn current_page(&self) -> Option<&GopherPage> {
        self.current_page.as_ref()
    }

    /// Loads a new `GopherPage`, caching decoded lines or GPU image textures once.
    pub fn set_page(&mut self, ctx: &egui::Context, page: GopherPage) {
        self.selected_all = false;
        self.pending_scroll_y = Some(0.0);
        self.focused_link_idx = None;
        self.scroll_to_focused = false;
        self.scroll_offset_y = 0.0;
        self.visible_rows = 0..0;

        self.cached = match page.content_type() {
            GopherItemType::GopherMenu => CachedContent::Menu,
            GopherItemType::ImageFile | GopherItemType::GifFile => {
                Self::decode_image_texture(ctx, &page)
            }
            _ => {
                let cleaned = page.text_content();
                let lines = cleaned.lines().map(str::to_owned).collect();
                CachedContent::Text { lines }
            }
        };

        self.current_page = Some(page);
    }

    /// Marks all text on the current page as selected and clears any partial cursor selection.
    pub fn select_all(&mut self, ctx: &egui::Context) {
        if self
            .current_page
            .as_ref()
            .is_some_and(|p| !p.content_type().is_image())
        {
            // Clear any active partial label selection so both states never overlap.
            egui::text_selection::LabelSelectionState::default().store(ctx);
            self.selected_all = true;
        }
    }

    /// Clears any active selection (both Select All and cursor drag selection) and link focus.
    pub fn clear_selection(&mut self, ctx: &egui::Context) {
        self.selected_all = false;
        self.focused_link_idx = None;
        egui::text_selection::LabelSelectionState::default().store(ctx);
    }

    /// Scrolls the active page view in response to keyboard navigation keys.
    pub fn scroll(&mut self, cmd: ScrollCommand) {
        let max_y = (self.content_height - self.viewport_height).max(0.0);
        let line_step = 66.0;
        let page_step = (self.viewport_height * 0.85).max(120.0);

        let target = match cmd {
            ScrollCommand::LineUp => (self.scroll_offset_y - line_step).max(0.0),
            ScrollCommand::LineDown => (self.scroll_offset_y + line_step).min(max_y),
            ScrollCommand::PageUp => (self.scroll_offset_y - page_step).max(0.0),
            ScrollCommand::PageDown => (self.scroll_offset_y + page_step).min(max_y),
            ScrollCommand::Top => 0.0,
            ScrollCommand::Bottom => max_y,
        };
        self.scroll_offset_y = target;
        self.pending_scroll_y = Some(target);
    }

    /// Moves keyboard focus to the next link in the current Gopher menu (wraps around).
    /// When no link in the visible viewport is focused, starts at the first visible link.
    pub fn focus_next_link(&mut self) {
        let Some(page) = &self.current_page else {
            return;
        };
        let items = page.items();
        if items.is_empty() {
            return;
        }

        let start = match self.focused_link_idx {
            Some(idx) if self.visible_rows.contains(&idx) => (idx + 1) % items.len(),
            _ => self.visible_rows.start.min(items.len() - 1),
        };

        for offset in 0..items.len() {
            let candidate = (start + offset) % items.len();
            if items[candidate].is_link() {
                self.focused_link_idx = Some(candidate);
                self.scroll_to_focused = !self.visible_rows.contains(&candidate);
                return;
            }
        }
    }

    /// Moves keyboard focus to the previous link in the current Gopher menu (wraps around).
    /// When no link in the visible viewport is focused, starts at the last visible link.
    pub fn focus_prev_link(&mut self) {
        let Some(page) = &self.current_page else {
            return;
        };
        let items = page.items();
        if items.is_empty() {
            return;
        }

        let start = match self.focused_link_idx {
            Some(idx) if self.visible_rows.contains(&idx) => {
                if idx == 0 {
                    items.len() - 1
                } else {
                    idx - 1
                }
            }
            _ => {
                if self.visible_rows.is_empty() {
                    items.len() - 1
                } else {
                    self.visible_rows
                        .end
                        .saturating_sub(1)
                        .min(items.len() - 1)
                }
            }
        };

        for offset in 0..items.len() {
            let candidate = (start + items.len() - offset) % items.len();
            if items[candidate].is_link() {
                self.focused_link_idx = Some(candidate);
                self.scroll_to_focused = !self.visible_rows.contains(&candidate);
                return;
            }
        }
    }

    /// Returns the currently keyboard-focused link item, if any.
    pub fn focused_link(&self) -> Option<GopherItem> {
        let idx = self.focused_link_idx?;
        self.current_page
            .as_ref()?
            .items()
            .get(idx)
            .filter(|item| item.is_link())
            .cloned()
    }

    /// Returns the currently selected text (full page text when Select All is active).
    pub fn selected_text(&self) -> String {
        if self.selected_all {
            self.current_page
                .as_ref()
                .map(GopherPage::text_content)
                .unwrap_or_default()
        } else {
            String::new()
        }
    }

    /// Renders the page view and returns any link click or context-menu action.
    pub fn show(
        &mut self,
        ui: &mut egui::Ui,
        theme: &Theme,
        selector_prefix_enabled: bool,
    ) -> Option<PageViewAction> {
        let mut action = None;

        Frame::new()
            .fill(theme.view_background)
            .inner_margin(Margin {
                left: 12,
                right: 0,
                top: 8,
                bottom: 8,
            })
            .show(ui, |ui| {
                ui.set_min_size(ui.available_size());
                // Use a predictable desktop-style solid scrollbar whose track matches the window background,
                // and unify cursor text selection color with Gophie's configured `page_selection` color.
                ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();
                let visuals = ui.visuals_mut();
                visuals.extreme_bg_color = theme.view_background;
                visuals.panel_fill = theme.view_background;
                visuals.selection.bg_fill = theme.page_selection;
                visuals.widgets.inactive.bg_stroke = egui::Stroke::NONE;
                visuals.widgets.hovered.bg_stroke = egui::Stroke::NONE;
                visuals.widgets.active.bg_stroke = egui::Stroke::NONE;

                // Left-clicking or starting a cursor drag unselects Select All and clears keyboard link focus.
                if ui.input(|i| i.pointer.primary_pressed()) {
                    self.selected_all = false;
                    self.focused_link_idx = None;
                }

                let selected_text = self.selected_text();
                let render_ctx = RenderContext {
                    theme,
                    selected_all: self.selected_all,
                    selected_text: &selected_text,
                    selector_prefix_enabled,
                    pending_scroll_y: self.pending_scroll_y.take(),
                    focused_link_idx: self.focused_link_idx,
                    scroll_to_focused: std::mem::take(&mut self.scroll_to_focused),
                };

                let Some(page) = &self.current_page else {
                    return;
                };

                let mut metrics: Option<ScrollMetrics> = None;

                match &self.cached {
                    CachedContent::Empty => {}
                    CachedContent::Menu => {
                        let (a, m) = Self::render_menu(ui, page, &render_ctx);
                        action = a;
                        metrics = Some(m);
                    }
                    CachedContent::Text { lines } => {
                        let (a, m) = Self::render_text(ui, page, lines, &render_ctx);
                        action = a;
                        metrics = Some(m);
                    }
                    CachedContent::Image { texture, size } => {
                        let mut scroll = ScrollArea::both()
                            .id_salt("gophie_image_scroll")
                            .auto_shrink([false, false]);
                        if let Some(y) = render_ctx.pending_scroll_y {
                            scroll = scroll.vertical_scroll_offset(y);
                        }
                        let out = scroll.show(ui, |ui| {
                            Self::render_image(ui, page, texture, *size, selector_prefix_enabled)
                        });
                        action = out.inner;
                        metrics = Some(ScrollMetrics {
                            offset_y: out.state.offset.y,
                            viewport_height: out.inner_rect.height(),
                            content_height: out.content_size.y,
                            visible_rows: 0..0,
                        });
                    }
                    CachedContent::ImageError(msg) => {
                        ui.label(
                            RichText::new(format!("Failed to display image:\n{}", msg))
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.view_text),
                        );
                    }
                }

                if let Some(m) = metrics {
                    self.scroll_offset_y = m.offset_y;
                    self.viewport_height = m.viewport_height;
                    self.content_height = m.content_height;
                    self.visible_rows = m.visible_rows;
                }
            });

        action
    }

    fn render_menu(
        ui: &mut egui::Ui,
        page: &GopherPage,
        rc: &RenderContext<'_>,
    ) -> (Option<PageViewAction>, ScrollMetrics) {
        let mut action = None;
        let theme = rc.theme;
        let items = page.items();
        let row_height = theme.page_font_size + 5.0;
        let digit_count = items.len().max(1).to_string().len().max(2);
        let line_num_width = (digit_count as f32) * (theme.page_font_size * 0.56) + 8.0;
        let icon_col_width = theme.page_icon_font_size + 12.0;

        // Must be set before `show_rows` so egui's virtualized height matches rendered rows exactly.
        ui.spacing_mut().item_spacing.y = 0.0;

        let mut scroll = ScrollArea::vertical()
            .id_salt("gophie_menu_rows")
            .auto_shrink([false, false]);
        if let Some(y) = rc.pending_scroll_y {
            scroll = scroll.vertical_scroll_offset(y);
        } else if rc.scroll_to_focused {
            if let Some(focused_idx) = rc.focused_link_idx {
                let target_y = (focused_idx as f32) * row_height;
                let centered = (target_y - ui.available_height() * 0.35).max(0.0);
                scroll = scroll.vertical_scroll_offset(centered);
            }
        }

        let mut visible_rows = 0..0;
        let out = scroll.show_rows(ui, row_height, items.len(), |ui, row_range| {
            visible_rows = row_range.clone();
            ui.spacing_mut().item_spacing.y = 0.0;

                for idx in row_range {
                    let item = &items[idx];
                    let line_number = idx + 1;

                    ui.horizontal(|ui| {
                        ui.set_height(row_height);
                        ui.spacing_mut().item_spacing.x = 4.0;

                        // Line number gutter (optional, non-selectable so dragging only selects page text)
                        if theme.show_line_numbers {
                            ui.allocate_ui_with_layout(
                                Vec2::new(line_num_width, row_height),
                                Layout::right_to_left(Align::Center),
                                |ui| {
                                    ui.add(
                                        egui::Label::new(
                                            RichText::new(line_number.to_string())
                                                .font(FontId::monospace(theme.page_font_size))
                                                .color(theme.page_line_number),
                                        )
                                        .selectable(false),
                                    );
                                },
                            );
                        }

                        // Feather item-type icon column (non-selectable)
                        ui.allocate_ui_with_layout(
                            Vec2::new(icon_col_width, row_height),
                            Layout::centered_and_justified(egui::Direction::LeftToRight),
                            |ui| {
                                let glyph = icons::for_item_type(item.item_type());
                                if !glyph.is_empty() {
                                    ui.add(
                                        egui::Label::new(
                                            RichText::new(glyph)
                                                .font(feather_font(theme.page_icon_font_size))
                                                .color(theme.view_text),
                                        )
                                        .selectable(false),
                                    );
                                }
                            },
                        );

                        // Item text or clickable link
                        let display = if item.display_text().is_empty() {
                            " "
                        } else {
                            item.display_text()
                        };

                        if item.is_link() {
                            let is_focused = rc.focused_link_idx == Some(idx);
                            let mut text = RichText::new(display)
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.page_link);
                            if rc.selected_all {
                                text = text.background_color(theme.page_selection);
                            }

                            let resp = ui
                                .add(egui::Label::new(text).sense(Sense::click()).extend())
                                .on_hover_cursor(CursorIcon::PointingHand);

                            if is_focused {
                                let focus_rect = resp.rect.expand2(Vec2::new(4.0, 1.0));
                                ui.painter().rect_stroke(
                                    focus_rect,
                                    egui::CornerRadius::same(3),
                                    egui::Stroke::new(1.5_f32, theme.page_link),
                                    egui::StrokeKind::Outside,
                                );
                            }

                            if resp.hovered() || is_focused {
                                let rect = resp.rect;
                                ui.painter().line_segment(
                                    [
                                        egui::pos2(rect.left(), rect.bottom() - 1.0),
                                        egui::pos2(rect.right(), rect.bottom() - 1.0),
                                    ],
                                    egui::Stroke::new(1.0_f32, theme.page_link),
                                );
                            }

                            if resp.clicked() {
                                action = Some(PageViewAction::LinkClicked(item.clone()));
                            }

                            resp.context_menu(|ui| {
                                if let Some(menu_act) = PageMenu::show_link_menu(
                                    ui,
                                    item,
                                    rc.selected_text,
                                    rc.selector_prefix_enabled,
                                ) {
                                    action = Some(PageViewAction::Menu(menu_act));
                                }
                            });
                        } else {
                            let mut text = RichText::new(display)
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.view_text);
                            if rc.selected_all {
                                text = text.background_color(theme.page_selection);
                            }

                            let resp = ui.add(
                                egui::Label::new(text)
                                    .sense(Sense::click())
                                    .extend(),
                            );
                            resp.context_menu(|ui| {
                                if let Some(menu_act) = PageMenu::show_page_menu(
                                    ui,
                                    page,
                                    rc.selected_text,
                                    rc.selector_prefix_enabled,
                                ) {
                                    action = Some(PageViewAction::Menu(menu_act));
                                }
                            });
                        }
                    });
                }
            });

        let metrics = ScrollMetrics {
            offset_y: out.state.offset.y,
            viewport_height: out.inner_rect.height(),
            content_height: out.content_size.y,
            visible_rows,
        };
        (action, metrics)
    }

    fn render_text(
        ui: &mut egui::Ui,
        page: &GopherPage,
        lines: &[String],
        rc: &RenderContext<'_>,
    ) -> (Option<PageViewAction>, ScrollMetrics) {
        let mut action = None;
        let theme = rc.theme;
        let row_height = theme.page_font_size + 4.0;

        // Must be set before `show_rows` so egui's virtualized height matches rendered rows exactly.
        ui.spacing_mut().item_spacing.y = 0.0;

        let mut scroll = ScrollArea::vertical()
            .id_salt("gophie_text_rows")
            .auto_shrink([false, false]);
        if let Some(y) = rc.pending_scroll_y {
            scroll = scroll.vertical_scroll_offset(y);
        }

        let mut visible_rows = 0..0;
        let out = scroll.show_rows(ui, row_height, lines.len().max(1), |ui, row_range| {
            visible_rows = row_range.clone();
            ui.spacing_mut().item_spacing.y = 0.0;
            for idx in row_range {
                let line = lines.get(idx).map(String::as_str).unwrap_or("");
                let display = if line.is_empty() { " " } else { line };
                let mut text = RichText::new(display)
                    .font(FontId::monospace(theme.page_font_size))
                    .color(theme.view_text);
                if rc.selected_all {
                    text = text.background_color(theme.page_selection);
                }

                ui.horizontal(|ui| {
                    ui.set_height(row_height);
                    let resp = ui.add(egui::Label::new(text).sense(Sense::click()).extend());
                    resp.context_menu(|ui| {
                        if let Some(menu_act) = PageMenu::show_page_menu(
                            ui,
                            page,
                            rc.selected_text,
                            rc.selector_prefix_enabled,
                        ) {
                            action = Some(PageViewAction::Menu(menu_act));
                        }
                    });
                });
            }
        });

        let metrics = ScrollMetrics {
            offset_y: out.state.offset.y,
            viewport_height: out.inner_rect.height(),
            content_height: out.content_size.y,
            visible_rows,
        };
        (action, metrics)
    }

    fn render_image(
        ui: &mut egui::Ui,
        page: &GopherPage,
        texture: &TextureHandle,
        original_size: Vec2,
        selector_prefix_enabled: bool,
    ) -> Option<PageViewAction> {
        let mut action = None;
        let max_width = ui.available_width().clamp(100.0, 800.0);
        let display_size = if original_size.x > max_width {
            let scale = max_width / original_size.x;
            Vec2::new(max_width, original_size.y * scale)
        } else {
            original_size
        };

        let resp = ui.add(
            egui::Image::new((texture.id(), display_size)).sense(Sense::click()),
        );
        resp.context_menu(|ui| {
            if let Some(menu_act) =
                PageMenu::show_page_menu(ui, page, "", selector_prefix_enabled)
            {
                action = Some(PageViewAction::Menu(menu_act));
            }
        });

        action
    }

    fn decode_image_texture(ctx: &egui::Context, page: &GopherPage) -> CachedContent {
        match image::load_from_memory(page.raw_bytes()) {
            Ok(img) => {
                let rgba = img.into_rgba8();
                let (w, h) = rgba.dimensions();
                let color_image = ColorImage::from_rgba_unmultiplied(
                    [w as usize, h as usize],
                    rgba.as_raw(),
                );
                let texture = ctx.load_texture(
                    format!("gophie_img_{}", page.url().to_url_string(false)),
                    color_image,
                    TextureOptions::LINEAR,
                );
                CachedContent::Image {
                    texture,
                    size: Vec2::new(w as f32, h as f32),
                }
            }
            Err(err) => CachedContent::ImageError(err.to_string()),
        }
    }
}
