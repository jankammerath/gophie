package net;

import java.util.ArrayList;

import net.GopherItem.GopherItemType;

/**
 * A GopherMenu page object that contains all information
 * and Gopher items provided in the underlying Gopher Menu
 */
public class GopherPage {
    private String sourceCode;
    private GopherUrl url;
    private ArrayList<GopherItem> itemList;
    private GopherItemType contentType = GopherItemType.UNKNOWN;

    /**
     * Constructs the GopherPage object and if it is 
     * a gopher menu or unknown it tries to parse it
     * as a gopher menu. If that fails, it will try
     * to evaluate the content type and store the 
     * source code.
     * 
     * @param gopherPageSourceCode
     * Source code or content of the gopher page
     * 
     * @param contentType
     * The estimated content type of the gopher page
     * 
     * @param gopherPageUrl
     * The URL of the gopher page
     */
    public GopherPage(String gopherPageSourceCode, GopherItemType contentType, GopherUrl gopherPageUrl){
        this.sourceCode = gopherPageSourceCode;
        this.url = gopherPageUrl;
        this.itemList = new ArrayList<GopherItem>();

        if(contentType == GopherItemType.GOPHERMENU 
            || contentType == GopherItemType.UNKNOWN){
            /* try to parse it as a gopher menu */
            try{
                /* execute the parse process */
                this.parse();

                /* parsing succeeded, define as gopher menu */
                this.contentType = GopherItemType.GOPHERMENU;
            }catch(Exception ex){
                /* parsing failed for whatever, define as text */
                this.contentType = GopherItemType.TEXTFILE;
            }
        }
    }

    /**
     * Returns the content type of this page
     * 
     * @return
     * The content type as gopher item type
     */
    public GopherItemType getContentType(){
        return this.contentType;
    }

    /**
     * Sets the source code (gophermap) of this gopher page
     * 
     * @param value
     * The text value as supplied by the server
     */
    public void setSourceCode(String value){
        this.sourceCode = value;
    }

    /**
     * Returns the source code (gophermap) of this page
     * 
     * @return
     * The gophermap content as a String
     */
    public String getSourceCode(){
        return this.sourceCode;
    }

    /* 
        Returns the GopherUrl object for this page
    */
    public GopherUrl getUrl(){
        return this.url;
    }

    /*
        Returns an array list with all gopher items of this page
    */
    public ArrayList<GopherItem> getItemList(){
        return this.itemList;
    }

    /* parses the local source code into components */
    private void parse(){
        String[] itemSourceList = this.sourceCode.split("\r\n");
        for(int i=0; i<itemSourceList.length; i++){
            String itemSource = itemSourceList[i];

            if(itemSource.length() > 0 && itemSource.equals(".") == false){
                this.itemList.add(new GopherItem(itemSource));
            }
        }
    }
}