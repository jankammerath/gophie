package org.gophie.net;

public class DownloadItem{
    GopherItem item;
    GopherClient client;
    String fileName;
    Boolean openFile = false;

    public DownloadItem(GopherItem gopherItem, String targetFile, Boolean openWhenFinished){
        this.client = new GopherClient();
        this.item = gopherItem;
        this.fileName = targetFile;
    }
}