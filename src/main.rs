//! Entry point for the Gophie desktop application.

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use eframe::egui::ViewportBuilder;
use gophie::ui::{assets, MainWindow, APPLICATION_TITLE};

fn main() -> eframe::Result<()> {
    let mut viewport = ViewportBuilder::default()
        .with_app_id("gophie")
        .with_title(APPLICATION_TITLE)
        .with_inner_size([960.0, 680.0])
        .with_min_inner_size([800.0, 600.0]);

    if let Some(icon) = assets::load_window_icon() {
        viewport = viewport.with_icon(icon);
    }

    let options = eframe::NativeOptions {
        viewport,
        centered: true,
        ..Default::default()
    };

    eframe::run_native(
        APPLICATION_TITLE,
        options,
        Box::new(|cc| Ok(Box::new(MainWindow::new(cc)))),
    )
}
