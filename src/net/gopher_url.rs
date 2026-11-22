//! RFC 1436 & RFC 4266 Gopher URL parsing and formatting.

/// Default TCP port for the Gopher protocol.
pub const DEFAULT_GOPHER_PORT: u16 = 70;

/// Represents a parsed Gopher URL (`gopher://host[:port][/selector]`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GopherUrl {
    host: String,
    port: u16,
    selector: String,
}

impl GopherUrl {
    /// Parses a Gopher URL string with or without the `gopher://` scheme.
    pub fn parse(raw: &str) -> Self {
        let trimmed = raw.trim();
        let without_scheme = trimmed.strip_prefix("gopher://").unwrap_or(trimmed);

        let (host_port, selector) = match without_scheme.find('/') {
            Some(idx) => (&without_scheme[..idx], &without_scheme[idx..]),
            None => (without_scheme, ""),
        };

        let (host, port) = match host_port.rsplit_once(':') {
            Some((h, p)) => match p.parse::<u16>() {
                Ok(parsed_port) => (h, parsed_port),
                Err(_) => (host_port, DEFAULT_GOPHER_PORT),
            },
            None => (host_port, DEFAULT_GOPHER_PORT),
        };

        Self {
            host: host.to_string(),
            port,
            selector: selector.to_string(),
        }
    }

    /// Constructs a `GopherUrl` from explicit components.
    pub fn from_parts(host: impl Into<String>, port: u16, selector: impl Into<String>) -> Self {
        Self {
            host: host.into(),
            port,
            selector: selector.into(),
        }
    }

    /// Returns the host name.
    pub fn host(&self) -> &str {
        &self.host
    }

    /// Returns the TCP port number (defaults to `70`).
    pub fn port(&self) -> u16 {
        self.port
    }

    /// Returns the raw selector string.
    pub fn selector(&self) -> &str {
        &self.selector
    }

    /// Returns the selector stripped of any leading RFC 4266 `/<type>/` prefix for wire requests.
    pub fn wire_selector(&self) -> &str {
        if self.has_type_prefix() {
            &self.selector[2..]
        } else {
            &self.selector
        }
    }

    /// Returns the RFC 4266 single-character item-type prefix if present (e.g. `/1/fun` -> `Some('1')`).
    pub fn type_prefix(&self) -> Option<char> {
        let bytes = self.selector.as_bytes();
        if bytes.len() >= 3 && bytes[0] == b'/' && bytes[2] == b'/' {
            let code = bytes[1] as char;
            if matches!(
                code,
                '0'..='9' | '+' | 'g' | 'I' | 'T' | 'h' | 'i' | 's' | '?'
            ) {
                return Some(code);
            }
        }
        None
    }

    /// Returns `true` if the selector starts with a valid RFC 4266 type prefix.
    pub fn has_type_prefix(&self) -> bool {
        self.type_prefix().is_some()
    }

    /// Sets or replaces the RFC 4266 item-type prefix on this URL's selector.
    pub fn set_type_prefix(&mut self, prefix: char) {
        if self.has_type_prefix() {
            let rest = &self.selector[3..];
            if rest.is_empty() {
                self.selector = format!("/{}/", prefix);
            } else if rest.starts_with('/') {
                self.selector = format!("/{}{}", prefix, rest);
            } else {
                self.selector = format!("/{}/{}", prefix, rest);
            }
        } else if self.selector.is_empty() {
            self.selector = format!("/{}/", prefix);
        } else if self.selector.starts_with('/') {
            self.selector = format!("/{}{}", prefix, self.selector);
        } else {
            self.selector = format!("/{}/{}", prefix, self.selector);
        }
    }

    /// Formats the URL string without the `gopher://` scheme.
    /// When `include_type_prefix` is `false`, any leading RFC 4266 `/<type>/` prefix is omitted.
    pub fn to_url_string(&self, include_type_prefix: bool) -> String {
        let mut result = self.host.clone();
        if self.port != DEFAULT_GOPHER_PORT {
            result.push(':');
            result.push_str(&self.port.to_string());
        }

        let selector_slice = if self.has_type_prefix() && !include_type_prefix {
            &self.selector[3..]
        } else {
            &self.selector
        };

        if !selector_slice.is_empty() {
            if selector_slice.starts_with('/') {
                result.push_str(selector_slice);
            } else {
                result.push('/');
                result.push_str(selector_slice);
            }
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_basic_and_prefixed_urls() {
        let url = GopherUrl::parse("gopher://gopher.floodgap.com/fun");
        assert_eq!(url.host(), "gopher.floodgap.com");
        assert_eq!(url.port(), 70);
        assert_eq!(url.selector(), "/fun");
        assert!(!url.has_type_prefix());
        assert_eq!(url.to_url_string(false), "gopher.floodgap.com/fun");

        let mut prefixed = GopherUrl::parse("gopher.floodgap.com:7070/1/fun");
        assert_eq!(prefixed.port(), 7070);
        assert_eq!(prefixed.type_prefix(), Some('1'));
        assert_eq!(prefixed.to_url_string(false), "gopher.floodgap.com:7070/fun");
        assert_eq!(prefixed.to_url_string(true), "gopher.floodgap.com:7070/1/fun");

        prefixed.set_type_prefix('0');
        assert_eq!(prefixed.to_url_string(true), "gopher.floodgap.com:7070/0/fun");
    }
}
