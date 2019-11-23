package org.gophie.net;

public class GopherItem{
    /* defines the official types of gopher items */
    public enum GopherItemType {
        /* Canonical types */
        TEXTFILE,                   // 0 = Text file
        GOPHERMENU,                 // 1 = Gopher (sub-)menu
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
        String[] property = line.replace("\r","").replace("\n","").split("\t");

        /* display string is first property without type code char */
        if(property.length > 0){ 
            if(property[0].length() > 1){
                this.userDisplayString = property[0].substring(1);
            }
        }

        /* second property is the selector */
        if(property.length > 1){ this.selector = property[1].trim(); }

        /* third property is the host name of the target */
        if(property.length > 2){ this.hostName = property[2].trim(); }

        /* fourth property is the port of the target host */
        if(property.length > 3){ 
            try{
                /* try to parse the port number */
                this.portNumber = Integer.parseInt(property[3].trim()); 
            }catch(Exception ex){
                /* report the failure */
                System.out.println("Found what was supposed to be a port number "
                    + "and it did not parse into an integer: " + ex.getMessage());
            }
        }
    }

    /**
     * Constructs an empty gopher item
     * 
     */
    public GopherItem(){
        /* nothing really happens here */
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

    /**
     * Creates a URL string for this item
     * 
     * @return
     * URL for this item as string
     */
    public String getUrlString(){
        String result = "";

        /* unknown or information links do not have
            any link associated with it */
        if(this.itemType != GopherItemType.UNKNOWN 
            && this.itemType != GopherItemType.INFORMATION){
            /* check if the selector contains a URL */
            if(this.selector.startsWith("URL:") == true
                || this.selector.startsWith("/URL:") == true){
                /* selector is link to other resource */
                if(this.selector.startsWith("/URL:")){
                    result = this.selector.substring(5);
                }else{
                    result = this.selector.substring(4);
                }
            }else{
                /* protocol is definitely gopher */
                result = "gopher://" + this.hostName;
                if(this.portNumber != 70){
                    result += ":" + this.portNumber;
                }

                /* add the slash to the URL if not present */
                if(!this.selector.startsWith("/")){
                    result += "/";
                }

                /* finally append the selector */
                result += this.selector;
            }
        }

        return result;
    }

    /**
     * Sets the code locally and also the proper type enum value 
     * 
     * @param code
     * the single-char gopher item type code
     */
    private void setItemTypeByCode(String code){
        this.itemTypeCode = code;
        if(code.equals("0")){ this.itemType = GopherItemType.TEXTFILE; }
        if(code.equals("1")){ this.itemType = GopherItemType.GOPHERMENU; } 
        if(code.equals("2")){ this.itemType = GopherItemType.CCSCO_NAMESERVER; } 
        if(code.equals("3")){ this.itemType = GopherItemType.ERRORCODE; } 
        if(code.equals("4")){ this.itemType = GopherItemType.BINHEX_FILE; } 
        if(code.equals("5")){ this.itemType = GopherItemType.DOS_FILE; } 
        if(code.equals("6")){ this.itemType = GopherItemType.UUENCODED_FILE; } 
        if(code.equals("7")){ this.itemType = GopherItemType.FULLTEXT_SEARCH; } 
        if(code.equals("8")){ this.itemType = GopherItemType.TELNET; } 
        if(code.equals("9")){ this.itemType = GopherItemType.BINARY_FILE; } 
        if(code.equals("+")){ this.itemType = GopherItemType.MIRROR; } 
        if(code.equals("g")){ this.itemType = GopherItemType.GIF_FILE; } 
        if(code.equals("I")){ this.itemType = GopherItemType.IMAGE_FILE; } 
        if(code.equals("T")){ this.itemType = GopherItemType.TELNET3270; } 
        if(code.equals("h")){ this.itemType = GopherItemType.HTML_FILE; }         
        if(code.equals("i")){ this.itemType = GopherItemType.INFORMATION; } 
        if(code.equals("s")){ this.itemType = GopherItemType.SOUND_FILE; } 
        if(code.equals("?")){ this.itemType = GopherItemType.UNKNOWN; } 
    }
}