package net;

import java.util.ArrayList;

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

    /* 
        Sets the goper menu source code for
        this gopher page as string

        @value      the gopermap source code as string
    */
    public void setSourceCode(String value){
        this.sourceCode = value;
    }

    /* 
        Returns the source code of this page
        in the gopher menu format
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

    /* parses the local source code into components */
    private void parse(){
        String[] itemSourceList = this.sourceCode.split("\r\n");
        for(int i=0; i<itemSourceList.length; i++){

        }
    }
}