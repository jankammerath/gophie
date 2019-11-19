package net;

public class GopherPage{
    private String sourceCode;

    public GopherPage(String gopherPageSourceCode){
        this.sourceCode = gopherPageSourceCode;
    }

    public void setSourceCode(String value){
        this.sourceCode = value;
    }

    public String getSourceCode(){
        return this.sourceCode;
    }

    /* parses the local source code into components */
    private void parse(){

    }
}