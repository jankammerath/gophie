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

        /* Gophie specific (Non-standard) */
        UNKNOWN;                     // any other unknown item type code
    }

    /* defines the type of this gopher item */
    private GopherItemType itemType = GopherItemType.UNKNOWN;

    /* defines the item type code of this gopher item */
    private String itemTypeCode = "?";

    /* the user display string of this gopher item */
    private String userDisplayString = "";

    /* defines the selector of this gopher item */
    private String selector = "";

    /* defines the host name of this gopher item */
    private String hostName = "";

    /* defines the port number of this gopher item */
    private int portNumber = 70;

    /* constructs the gopher item taking the single line
        and parsing its content into the structure of this
        object
        
        @line       the single gophermenu line for this item
    */
    public GopherItem(String line){
        /* type code is defined as first character in line */
        this.setItemTypeByCode(line.substring(0,1));
        
        /* get all properties for this item */
        String[] property = line.split("\t");

        /* display string is first property without type code char */
        this.userDisplayString = property[0].substring(1).trim();

        /* second property is the selector */
        if(property.length > 0){ this.selector = property[1].trim(); }

        /* third property is the host name of the target */
        if(property.length > 1){ this.hostName = property[2].trim(); }
    }

    /* 
        Returns the item type as GopherItemType enum
    */
    public GopherItemType getItemType(){
        return this.itemType;
    }

    /* 
        Returns the item type code as a string
    */
    public String getItemTypeCode(){
        return this.itemTypeCode;
    }

    /* 
        Returns the user display string which is 
        supposed to be used as the title of this
        gopher item when being displayed
    */
    public String getUserDisplayString(){
        return this.userDisplayString;
    }

    /*
        Returns the selector of this gopher item
    */
    public String getSelector(){
        return this.selector;
    }

    /* 
        Returns the item host name as a string
    */
    public String getHostName(){
        return this.hostName;
    }

    /* 
        Returns the port number for the gopher 
        item's host to collect the content from
    */
    public int getPortNumber(){
        return this.portNumber;
    }

    /* 
        sets the code locally and also the proper type enum value

        @code           the single-char gopher item type code
    */
    private void setItemTypeByCode(String code){
        this.itemTypeCode = code;
        if(code == "0"){ this.itemType = GopherItemType.TEXTFILE; }
        if(code == "1"){ this.itemType = GopherItemType.SUBMENU; } 
        if(code == "2"){ this.itemType = GopherItemType.CCSCO_NAMESERVER; } 
        if(code == "3"){ this.itemType = GopherItemType.ERRORCODE; } 
        if(code == "4"){ this.itemType = GopherItemType.BINHEX_FILE; } 
        if(code == "5"){ this.itemType = GopherItemType.DOS_FILE; } 
        if(code == "6"){ this.itemType = GopherItemType.UUENCODED_FILE; } 
        if(code == "7"){ this.itemType = GopherItemType.FULLTEXT_SEARCH; } 
        if(code == "8"){ this.itemType = GopherItemType.TELNET; } 
        if(code == "9"){ this.itemType = GopherItemType.BINARY_FILE; } 
        if(code == "+"){ this.itemType = GopherItemType.MIRROR; } 
        if(code == "g"){ this.itemType = GopherItemType.GIF_FILE; } 
        if(code == "I"){ this.itemType = GopherItemType.IMAGE_FILE; } 
        if(code == "T"){ this.itemType = GopherItemType.TELNET3270; } 
        if(code == "h"){ this.itemType = GopherItemType.HTML_FILE; }         
        if(code == "i"){ this.itemType = GopherItemType.INFORMATION; } 
        if(code == "s"){ this.itemType = GopherItemType.SOUND_FILE; } 
        if(code == "?"){ this.itemType = GopherItemType.UNKNOWN; } 
    }
}