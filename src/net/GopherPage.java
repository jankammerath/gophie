package net;

import java.util.ArrayList;

/**
 * A GopherMenu page object that contains all information
 * and Gopher items provided in the underlying Gopher Menu
 */
public class GopherPage {
    private String sourceCode;
    private GopherUrl url;
    private ArrayList<GopherItem> itemList;

    public GopherPage(String gopherPageSourceCode, GopherUrl gopherPageUrl){
        this.sourceCode = gopherPageSourceCode;
        this.url = gopherPageUrl;
        this.itemList = new ArrayList<GopherItem>();
        this.parse();
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