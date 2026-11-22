//! File-type detection via magic byte signatures and text heuristics.

/// Coarse file category inferred from the leading bytes of a Gopher payload.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FileSignatureType {
    Unknown,
    Binary,
    Image,
    Media,
    Text,
}

/// Identifies a file's category from its raw byte header without extra heap allocations.
pub struct FileSignature<'a> {
    content: &'a [u8],
}

impl<'a> FileSignature<'a> {
    const IMAGE_SIGNATURES: &'static [&'static [u8]] = &[
        b"GIF8",                             // GIF
        &[0xFF, 0xD8, 0xFF],                 // JPEG
        &[0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20], // JPEG 2000
        &[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A], // PNG
        b"BM",                               // BMP
        &[0x00, 0x00, 0x01, 0x00],           // ICO
        &[0x49, 0x20, 0x49],                 // TIFF
        &[0x49, 0x49, 0x2A, 0x00],           // TIFF LE
        &[0x4D, 0x4D, 0x00, 0x2A],           // TIFF BE
        &[0x4D, 0x4D, 0x00, 0x2B],           // BigTIFF
        b"RIFF",                             // Checked with WEBP below
    ];

    const MEDIA_SIGNATURES: &'static [&'static [u8]] = &[
        b"FORM",                                           // AIFF
        b"OggS",                                           // OGG
        b"ID3",                                            // MP3 (ID3)
        &[0xFF, 0xFA],                                     // MP3
        &[0xFF, 0xFB],                                     // MP3
        &[0xFF, 0xF3],                                     // MP3
        &[0xFF, 0xF2],                                     // MP3
        b"RIFF",                                           // WAV / AVI
        b"MThd",                                           // MIDI
        b"fLaC",                                           // FLAC
        &[0x1A, 0x45, 0xDF, 0xA3],                         // MKV / WebM
        &[0x00, 0x00, 0x01, 0xBA],                         // MPEG
        &[0x00, 0x00, 0x01, 0xB3],                         // MPEG
        &[0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11], // WMV/WMA/ASF
        &[0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C], // ASF
    ];

    const BINARY_SIGNATURES: &'static [&'static [u8]] = &[
        &[0x52, 0x61, 0x72, 0x21, 0x1A, 0x07],             // RAR
        &[0x50, 0x4B, 0x03, 0x04],                         // ZIP
        &[0x50, 0x4B, 0x05, 0x06],                         // ZIP empty
        &[0x50, 0x4B, 0x07, 0x08],                         // ZIP spanned
        &[0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C],             // 7z
        &[0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00],             // XZ
        &[0x04, 0x22, 0x4D, 0x18],                         // LZ4
        &[0x4D, 0x53, 0x43, 0x46],                         // CAB
        &[0x53, 0x5A, 0x44, 0x44, 0x88, 0xF0, 0x27, 0x33], // SZDD
        &[0x28, 0x54, 0x68, 0x69],                         // HQX
        &[0x75, 0x73, 0x74, 0x61, 0x72, 0x00, 0x30, 0x30], // TAR
        &[0x75, 0x73, 0x74, 0x61, 0x72, 0x20, 0x20, 0x00], // TAR
        &[0x1F, 0x8B],                                     // GZIP
        &[0x78, 0x01, 0x73, 0x0D, 0x62, 0x62, 0x60],       // DMG
        &[0x53, 0x49, 0x54, 0x21, 0x00],                   // SIT
        b"StuffIt ",                                       // StuffIt
        &[0x4C, 0x00, 0x00, 0x00, 0x01, 0x14, 0x02, 0x00], // Windows LNK
    ];

    /// Creates a new `FileSignature` inspector over `content`.
    pub fn new(content: &'a [u8]) -> Self {
        Self { content }
    }

    /// Detects the file signature type using magic headers and ASCII text heuristics.
    pub fn detect(&self) -> FileSignatureType {
        if self.content.is_empty() {
            return FileSignatureType::Unknown;
        }

        // Check WebP before generic RIFF media signatures.
        if self.content.len() >= 12
            && &self.content[0..4] == b"RIFF"
            && &self.content[8..12] == b"WEBP"
        {
            return FileSignatureType::Image;
        }

        // Check MP4/ISO BMFF container ("ftyp" at offset 4).
        if self.content.len() >= 8 && &self.content[4..8] == b"ftyp" {
            return FileSignatureType::Media;
        }

        if self.matches_any(Self::BINARY_SIGNATURES) {
            return FileSignatureType::Binary;
        }
        if self.matches_any(Self::MEDIA_SIGNATURES) {
            return FileSignatureType::Media;
        }
        if self.matches_image() {
            return FileSignatureType::Image;
        }
        if self.is_text_content() {
            return FileSignatureType::Text;
        }

        FileSignatureType::Unknown
    }

    fn matches_image(&self) -> bool {
        // Exclude generic RIFF here (already checked for WEBP above).
        Self::IMAGE_SIGNATURES
            .iter()
            .filter(|sig| *sig != b"RIFF")
            .any(|sig| self.content.starts_with(sig))
    }

    fn matches_any(&self, signatures: &[&[u8]]) -> bool {
        signatures.iter().any(|sig| self.content.starts_with(sig))
    }

    /// Returns true if the payload looks like human-readable text (>= 20% ASCII letters).
    fn is_text_content(&self) -> bool {
        let mut alpha_count: usize = 0;
        let mut non_zero_count: usize = 0;

        for &byte in self.content {
            if byte != 0 {
                if byte.is_ascii_alphabetic() {
                    alpha_count += 1;
                }
                non_zero_count += 1;
            }
        }

        if non_zero_count == 0 {
            return false;
        }

        (alpha_count as f64 / non_zero_count as f64) * 100.0 >= 20.0
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_common_signatures() {
        assert_eq!(
            FileSignature::new(&[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]).detect(),
            FileSignatureType::Image
        );
        assert_eq!(
            FileSignature::new(b"GIF89a...").detect(),
            FileSignatureType::Image
        );
        assert_eq!(
            FileSignature::new(b"ID3\x03\x00\x00\x00").detect(),
            FileSignatureType::Media
        );
        assert_eq!(
            FileSignature::new(&[0x00, 0x00, 0x00, 0x18, b'f', b't', b'y', b'p']).detect(),
            FileSignatureType::Media
        );
        assert_eq!(
            FileSignature::new(&[0x50, 0x4B, 0x03, 0x04, 0x14, 0x00]).detect(),
            FileSignatureType::Binary
        );
        assert_eq!(
            FileSignature::new(b"Welcome to Floodgap Systems' official gopher server.").detect(),
            FileSignatureType::Text
        );
    }
}
