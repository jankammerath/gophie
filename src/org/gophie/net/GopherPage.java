package org.gophie.net;

import java.util.ArrayList;
import java.util.Base64;

import org.gophie.net.GopherItem.GopherItemType;

/**
 * A GopherMenu page object that contains all information
 * and Gopher items provided in the underlying Gopher Menu
 */
public class GopherPage {
    /* defines the default charset */
    private static final String GOPHERPAGE_DEFAULT_CHARSET = "UTF-8";

    /* local variables */
    private byte[] sourceCode;
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
     * @param gopherContentType
     * The estimated content type of the gopher page
     * 
     * @param gopherPageUrl
     * The URL of the gopher page
     */
    public GopherPage(byte[] gopherPageSourceCode, GopherItemType gopherContentType, GopherUrl gopherPageUrl){
        this.sourceCode = gopherPageSourceCode;
        this.url = gopherPageUrl;
        this.itemList = new ArrayList<GopherItem>();

        if(gopherContentType == GopherItemType.GOPHERMENU 
            || gopherContentType == GopherItemType.UNKNOWN){
            /* try to parse it as a gopher menu */
            try{
                /* execute the parse process */
                this.parse();

                /* parsing succeeded, define as gopher menu */
                this.contentType = GopherItemType.GOPHERMENU;
            }catch(Exception ex){
                /* output the parser exception */
                System.out.println("Failed to parse gophermenu: " + ex.getMessage());
                ex.printStackTrace();

                /* parsing failed for whatever, define as text */
                this.contentType = GopherItemType.TEXTFILE;
            }
        }else{
            /* set the supplied content type */
            this.contentType = gopherContentType;
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
     * Returns the source code in base64 encoded format
     * which can be used to display images in the view
     * 
     * @return
     * String with base64 encoded data of the source code
     */
    public String getBase64(){
        return Base64.getEncoder().encodeToString(this.sourceCode); 
    }

    /**
     * Returns the raw bytes of the data received
     * 
     * @return
     * Byte array with the raw gopher page data
     */
    public byte[] getByteArray(){
        return this.sourceCode;
    }

    /**
     * Sets the source code (gophermap) of this gopher page
     * 
     * @param value
     * The text value as supplied by the server
     */
    public void setSourceCode(byte[] value){
        this.sourceCode = value;
    }

    /**
     * Returns the source code (gophermap) of this page
     * 
     * @return
     * The gophermap content as a String
     */
    public String getSourceCode(){
        try{
            return new String(this.sourceCode, GOPHERPAGE_DEFAULT_CHARSET);
        }catch(Exception ex){
            /* drop a quick info on the console when decoding fails */
            System.out.println("Failed to decode bytes of Gopher Page: " + ex.getMessage());
            return "";
        }
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
        String[] itemSourceList = this.getSourceCode().split("\n");
        for(int i=0; i<itemSourceList.length; i++){
            String itemSource = itemSourceList[i];

            if(itemSource.length() > 0 && itemSource.equals(".") == false){
                this.itemList.add(new GopherItem(itemSource));
            }
        }
    }
}