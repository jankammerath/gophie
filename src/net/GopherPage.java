package net;

import java.util.ArrayList;

public class GopherPage {
    private String sourceCode;
    private ArrayList<GopherItem> itemList;

    public GopherPage(String gopherPageSourceCode){
        this.sourceCode = gopherPageSourceCode;
        this.itemList = new ArrayList<GopherItem>();
    }

    public void setSourceCode(String value){
        this.sourceCode = value;
    }

    public String getSourceCode(){
        return this.sourceCode;
    }

    /* parses the local source code into components */
    private void parse(){
        String[] itemSourceList = this.sourceCode.split("\r\n");
        for(int i=0; i<itemSourceList.length; i++){
            
        }
    }
}