//! Gopher `caps.txt` capability parser, path breadcrumb resolver, and per-host TTL cache.

use std::collections::{BTreeMap, HashMap, HashSet};
use std::time::{Duration, Instant};

const DEFAULT_CACHE_TTL_SECS: u64 = 3600;
const NEGATIVE_CACHE_TTL_SECS: u64 = 600;

/// A single navigable segment in a `caps.txt`-derived selector breadcrumb trail.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Breadcrumb {
    pub label: String,
    pub selector: String,
}

/// Parsed representation of a Gopher server's `caps.txt` file.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GopherCaps {
    pub caps_version: Option<u32>,
    pub expire_after_secs: u64,

    pub path_delimiter: Option<String>,
    pub path_identity: String,
    pub path_parent: String,
    pub path_parent_double: bool,
    pub path_escape_char: Option<char>,
    pub path_keep_pre_delimiter: bool,

    pub server_software: Option<String>,
    pub server_software_version: Option<String>,
    pub server_architecture: Option<String>,
    pub server_description: Option<String>,
    pub server_geolocation: Option<String>,
    pub server_tls_port: Option<u16>,
    pub server_supports_stdin_scripts: bool,
    pub server_admin: Option<String>,

    /// Any additional or custom key/value pairs present in `caps.txt`.
    pub extra: BTreeMap<String, String>,
}

impl Default for GopherCaps {
    fn default() -> Self {
        Self {
            caps_version: None,
            expire_after_secs: DEFAULT_CACHE_TTL_SECS,
            path_delimiter: None,
            path_identity: String::from("."),
            path_parent: String::from(".."),
            path_parent_double: false,
            path_escape_char: None,
            path_keep_pre_delimiter: false,
            server_software: None,
            server_software_version: None,
            server_architecture: None,
            server_description: None,
            server_geolocation: None,
            server_tls_port: None,
            server_supports_stdin_scripts: false,
            server_admin: None,
            extra: BTreeMap::new(),
        }
    }
}

impl GopherCaps {
    /// Parses raw `caps.txt` text.
    /// Returns `None` if the payload does not begin with the required `CAPS` signature.
    pub fn parse(raw: &str) -> Option<Self> {
        let mut lines = raw.lines();
        let first_line = lines.next()?.trim_end_matches('\r');
        if !first_line.starts_with("CAPS") {
            return None;
        }

        let mut caps = Self::default();

        for raw_line in lines {
            let line = raw_line.trim_end_matches('\r').trim();
            if line.is_empty() || line.starts_with('#') || line == "." {
                continue;
            }
            let Some((raw_key, raw_val)) = line.split_once('=') else {
                continue;
            };

            let key = raw_key.trim();
            let val = raw_val.trim();
            if key.is_empty() {
                continue;
            }

            match key {
                "CapsVersion" => {
                    caps.caps_version = val.parse::<u32>().ok();
                }
                "ExpireCapsAfter" => {
                    if let Ok(secs) = val.parse::<u64>() {
                        caps.expire_after_secs = secs;
                    }
                }
                "PathDelimiter" | "PathDelimeter" => {
                    if !val.is_empty() {
                        caps.path_delimiter = Some(val.to_string());
                    }
                }
                "PathIdentity" => {
                    if !val.is_empty() {
                        caps.path_identity = val.to_string();
                    }
                }
                "PathParent" => {
                    if !val.is_empty() {
                        caps.path_parent = val.to_string();
                    }
                }
                "PathParentDouble" => {
                    caps.path_parent_double = parse_caps_bool(val);
                }
                "PathEscapeCharacter" => {
                    caps.path_escape_char = val.chars().next();
                }
                "PathKeepPreDelimiter" | "PathKeepPreDelimeter" => {
                    caps.path_keep_pre_delimiter = parse_caps_bool(val);
                }
                "ServerSoftware" => caps.server_software = non_empty(val),
                "ServerSoftwareVersion" => caps.server_software_version = non_empty(val),
                "ServerArchitecture" => caps.server_architecture = non_empty(val),
                "ServerDescription" => caps.server_description = non_empty(val),
                "ServerGeolocationString" => caps.server_geolocation = non_empty(val),
                "ServerTLSPort" => caps.server_tls_port = val.parse::<u16>().ok(),
                "ServerSupportsStdinScripts" => {
                    caps.server_supports_stdin_scripts = parse_caps_bool(val);
                }
                "ServerAdmin" => caps.server_admin = non_empty(val),
                other => {
                    if !val.is_empty() {
                        caps.extra.insert(other.to_string(), val.to_string());
                    }
                }
            }
        }

        Some(caps)
    }

    /// Returns the recommended cache lifetime for this `caps.txt`.
    pub fn cache_ttl(&self) -> Duration {
        Duration::from_secs(self.expire_after_secs.max(30))
    }

    /// Splits and normalizes `selector` into navigable breadcrumbs using the server's path rules.
    /// Returns an empty list if `PathDelimiter` is unspecified (opaque selectors) or at root.
    pub fn breadcrumbs(&self, selector: &str) -> Vec<Breadcrumb> {
        let Some(delim) = self.path_delimiter.as_deref() else {
            return Vec::new();
        };
        if delim.is_empty() || selector.is_empty() {
            return Vec::new();
        }

        // Ignore query portion after TAB (Type-7 search selectors).
        let path_part = selector.split('\t').next().unwrap_or(selector);
        let raw_tokens = split_escaped(path_part, delim, self.path_escape_char);

        let mut segments: Vec<String> = Vec::new();
        let mut saw_non_empty = false;

        for token in raw_tokens {
            if token.is_empty() {
                if self.path_parent_double && saw_non_empty {
                    segments.pop();
                }
                continue;
            }
            saw_non_empty = true;

            if token == self.path_identity {
                continue;
            }
            if token == self.path_parent {
                segments.pop();
                continue;
            }
            segments.push(token);
        }

        if segments.is_empty() {
            return Vec::new();
        }

        let mut crumbs = Vec::with_capacity(segments.len());
        let mut acc = String::new();
        for (idx, seg) in segments.iter().enumerate() {
            if idx == 0 {
                if !self.path_keep_pre_delimiter {
                    acc.push_str(delim);
                }
                acc.push_str(seg);
            } else {
                acc.push_str(delim);
                acc.push_str(seg);
            }
            crumbs.push(Breadcrumb {
                label: seg.clone(),
                selector: acc.clone(),
            });
        }
        crumbs
    }

    /// Formats a human-readable software label (e.g. `"Bucktooth 0.2.9"`).
    pub fn software_display(&self) -> Option<String> {
        match (&self.server_software, &self.server_software_version) {
            (Some(sw), Some(ver)) => Some(format!("{} {}", sw, ver)),
            (Some(sw), None) => Some(sw.clone()),
            _ => None,
        }
    }

    /// Returns key-value metadata rows suitable for displaying in a Server Info panel.
    pub fn info_rows(&self) -> Vec<(String, String)> {
        let mut rows = Vec::new();
        if let Some(sw) = self.software_display() {
            rows.push(("Software".into(), sw));
        }
        if let Some(arch) = &self.server_architecture {
            rows.push(("Architecture".into(), arch.clone()));
        }
        if let Some(desc) = &self.server_description {
            rows.push(("Description".into(), desc.clone()));
        }
        if let Some(geo) = &self.server_geolocation {
            rows.push(("Location".into(), geo.clone()));
        }
        if let Some(admin) = &self.server_admin {
            rows.push(("Admin".into(), admin.clone()));
        }
        if let Some(tls_port) = self.server_tls_port {
            rows.push(("TLS Port".into(), tls_port.to_string()));
        }
        if self.server_supports_stdin_scripts {
            rows.push(("Stdin Scripts".into(), "Supported".into()));
        }
        if let Some(delim) = &self.path_delimiter {
            rows.push(("Path Delimiter".into(), delim.clone()));
        }
        for (k, v) in &self.extra {
            rows.push((k.clone(), v.clone()));
        }
        rows
    }
}

struct CachedCapsEntry {
    caps: Option<GopherCaps>,
    fetched_at: Instant,
    ttl: Duration,
}

impl CachedCapsEntry {
    fn is_expired(&self) -> bool {
        self.fetched_at.elapsed() >= self.ttl
    }
}

/// Per-host in-memory cache of `caps.txt` responses respecting `ExpireCapsAfter`.
#[derive(Default)]
pub struct CapsCache {
    entries: HashMap<(String, u16), CachedCapsEntry>,
    in_flight: HashSet<(String, u16)>,
}

impl CapsCache {
    pub fn new() -> Self {
        Self::default()
    }

    /// Returns cached `GopherCaps` for `(host, port)` if present and not expired.
    pub fn get(&self, host: &str, port: u16) -> Option<&GopherCaps> {
        let key = (host.to_ascii_lowercase(), port);
        let entry = self.entries.get(&key)?;
        if entry.is_expired() {
            None
        } else {
            entry.caps.as_ref()
        }
    }

    /// Returns `true` if `(host, port)` should trigger a background `caps.txt` fetch.
    pub fn needs_fetch(&self, host: &str, port: u16) -> bool {
        if host.is_empty() {
            return false;
        }
        let key = (host.to_ascii_lowercase(), port);
        if self.in_flight.contains(&key) {
            return false;
        }
        match self.entries.get(&key) {
            Some(entry) => entry.is_expired(),
            None => true,
        }
    }

    /// Marks `(host, port)` as currently fetching to prevent duplicate requests.
    pub fn mark_in_flight(&mut self, host: &str, port: u16) {
        self.in_flight.insert((host.to_ascii_lowercase(), port));
    }

    /// Stores a completed `caps.txt` fetch result (or negative cache entry if unavailable).
    pub fn insert(&mut self, host: &str, port: u16, caps: Option<GopherCaps>) {
        let key = (host.to_ascii_lowercase(), port);
        self.in_flight.remove(&key);
        let ttl = caps
            .as_ref()
            .map(GopherCaps::cache_ttl)
            .unwrap_or_else(|| Duration::from_secs(NEGATIVE_CACHE_TTL_SECS));
        self.entries.insert(
            key,
            CachedCapsEntry {
                caps,
                fetched_at: Instant::now(),
                ttl,
            },
        );
    }
}

fn parse_caps_bool(val: &str) -> bool {
    matches!(
        val.trim().to_ascii_uppercase().as_str(),
        "TRUE" | "YES" | "1" | "ON"
    )
}

fn non_empty(val: &str) -> Option<String> {
    if val.is_empty() {
        None
    } else {
        Some(val.to_string())
    }
}

fn split_escaped(input: &str, delim: &str, escape: Option<char>) -> Vec<String> {
    if escape.is_none() || delim.len() != 1 {
        return input.split(delim).map(str::to_string).collect();
    }

    let esc = escape.unwrap();
    let delim_char = delim.chars().next().unwrap();
    let mut parts = Vec::new();
    let mut current = String::new();
    let mut chars = input.chars().peekable();

    while let Some(ch) = chars.next() {
        if ch == esc {
            if let Some(next_ch) = chars.next() {
                current.push(next_ch);
            }
        } else if ch == delim_char {
            parts.push(std::mem::take(&mut current));
        } else {
            current.push(ch);
        }
    }
    parts.push(current);
    parts
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE_CAPS: &str = "\
CAPS
# Comment line
CapsVersion=1
ExpireCapsAfter=3600
PathDelimeter=/
PathDelimiter=/
PathIdentity=.
PathParent=..
PathParentDouble=FALSE
PathEscapeCharacter=\\
PathKeepPreDelimeter=FALSE
PathKeepPreDelimiter=FALSE
ServerSoftware=Bucktooth
ServerSoftwareVersion=0.2.9
ServerArchitecture=AIX
ServerDescription=IBM Power 520 Express, dual 4.2GHz POWER6 CPU, 8GB RAM
ServerGeolocationString=Southern California, USA
ServerTLSPort=7443
ServerSupportsStdinScripts=TRUE
ServerAdmin=gopher@floodgap.com
.\r\n";

    #[test]
    fn parses_floodgap_caps_file() {
        let caps = GopherCaps::parse(SAMPLE_CAPS).expect("should parse valid CAPS file");
        assert_eq!(caps.caps_version, Some(1));
        assert_eq!(caps.expire_after_secs, 3600);
        assert_eq!(caps.path_delimiter.as_deref(), Some("/"));
        assert_eq!(caps.path_identity, ".");
        assert_eq!(caps.path_parent, "..");
        assert!(!caps.path_parent_double);
        assert_eq!(caps.path_escape_char, Some('\\'));
        assert!(!caps.path_keep_pre_delimiter);
        assert_eq!(caps.software_display().as_deref(), Some("Bucktooth 0.2.9"));
        assert_eq!(caps.server_architecture.as_deref(), Some("AIX"));
        assert_eq!(caps.server_tls_port, Some(7443));
        assert!(caps.server_supports_stdin_scripts);
        assert_eq!(caps.server_admin.as_deref(), Some("gopher@floodgap.com"));
    }

    #[test]
    fn rejects_non_caps_response() {
        assert!(GopherCaps::parse("3caps.txt not found\tfake\terror.host\t1").is_none());
        assert!(GopherCaps::parse("").is_none());
    }

    #[test]
    fn builds_posix_breadcrumbs_with_normalization() {
        let caps = GopherCaps::parse(SAMPLE_CAPS).unwrap();
        let crumbs = caps.breadcrumbs("/gopher/./clients/../fun//comics");
        assert_eq!(
            crumbs,
            vec![
                Breadcrumb {
                    label: "gopher".into(),
                    selector: "/gopher".into(),
                },
                Breadcrumb {
                    label: "fun".into(),
                    selector: "/gopher/fun".into(),
                },
                Breadcrumb {
                    label: "comics".into(),
                    selector: "/gopher/fun/comics".into(),
                },
            ]
        );
    }

    #[test]
    fn supports_mac_hfs_path_parent_double() {
        let hfs = "CAPS\nPathDelimiter=:\nPathParentDouble=TRUE\nPathKeepPreDelimiter=TRUE\n";
        let caps = GopherCaps::parse(hfs).unwrap();
        // MacHD:x:y:::z -> MacHD:z
        let crumbs = caps.breadcrumbs("MacHD:x:y:::z");
        assert_eq!(
            crumbs,
            vec![
                Breadcrumb {
                    label: "MacHD".into(),
                    selector: "MacHD".into(),
                },
                Breadcrumb {
                    label: "z".into(),
                    selector: "MacHD:z".into(),
                },
            ]
        );
    }

    #[test]
    fn handles_opaque_selectors_without_delimiter() {
        let caps = GopherCaps::parse("CAPS\nCapsVersion=1\n").unwrap();
        assert!(caps.breadcrumbs("/a/b/c").is_empty());
    }
}
