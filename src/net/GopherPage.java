package net;

public class GopherPage{
    private String textContent;

    public void setTextContent(String value){
        this.textContent = value;
    }

    public String getTextContent(){
        return this.textContent;
    }

    public static GopherPage parse(String text){
        return new GopherPage();
    }
}