//! Graphical user interface components built with `eframe` / `egui`.

pub mod assets;
mod download_window;
mod main_window;
mod message_view;
mod navigation_bar;
mod page_menu;
mod page_view;
mod search_input;
mod settings_window;

pub use main_window::{MainWindow, APPLICATION_TITLE};
