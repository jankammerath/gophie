package net;

public class GopherItem{
    /* defines the official types of gopher items */
    public enum GopherItemType {
        /* Canonical types */
        TEXTFILE,                   // 0 = Text file
        SUBMENU,                    // 1 = Gopher submenu
        CCSCO_NAMESERVER,           // 2 = CCSO Nameserver
        ERRORCODE,                  // 3 = Error code returned by a Gopher server to indicate failure
        BINHEX_FILE,                // 4 = BinHex-encoded file (primarily for Macintosh computers)
        DOS_FILE,                   // 5 = DOS file
        UUENCODED_FILE,             // 6 = uuencoded file
        FULLTEXT_SEARCH,            // 7 = Gopher full-text search
        TELNET,                     // 8 = Telnet
        BINARY_FILE,                // 9 = Binary file
        MIRROR,                     // + = Mirror or alternate server (load balance or failover)
        GIF_FILE,                   // g = GIF file
        IMAGE_FILE,                 // I = Image file
        TELNET3270,                 // T = Telnet 3270

        /* Non-canonical types */
        HTML_FILE,                  // h = HTML file
        INFORMATION,                // i = Informational message
        SOUND_FILE,                 // s = Sound file (especially the WAV format)

        /* Gophie specific */
        UNKNOWN                     // any other unknown item type code
    }

    private GopherItemType itemType;

    public GopherItem(String line){
        
    }
}