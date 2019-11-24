package org.gophie.net;

import java.awt.*;
import java.io.File;

import org.gophie.net.event.*;

public class DownloadItem implements GopherClientEventListener {
    public enum DownloadStatus {
        IDLE,
        FAILED,
        ACTIVE,
        COMPLETED
    }

    /* local objects and variables */
    private GopherItem item;
    private GopherClient client;
    private String fileName;
    private Boolean openFile = false;
    private long byteCountLoaded = 0;
    private DownloadStatus status = DownloadStatus.IDLE;

    /**
     * Constructor creates the download 
     * and starts it immediately
     * 
     * @param gopherItem
     * The gopher item to download
     * 
     * @param targetFile
     * The file to write the contents to
     * 
     * @param openWhenFinished
     * If true, opens the file when finished
     */
    public DownloadItem(GopherItem gopherItem, String targetFile, Boolean openWhenFinished) {
        this.client = new GopherClient();
        this.item = gopherItem;
        this.fileName = targetFile;
        this.openFile = openWhenFinished;
        this.start();
    }

    /**
     * Constructor creates the item with
     * default values and does not do anything
     * 
     */
    public DownloadItem(){
        this.client = new GopherClient();
    }

    /**
     * Starts the download of the file
     */
    public void start(){
        /* start the download process */
        this.client.downloadAsync(this.item.getUrlString(), this.fileName, this);
        this.status = DownloadStatus.ACTIVE;
    }

    /**
     * Sets the target file to download to
     * 
     * @param targetFile
     * Path of the file to store data in
     */
    public void setTargetFile(String targetFile){
        this.fileName = targetFile;
    }

    /**
     * Sets the gopher item
     * 
     * @param gopherItem
     * The gopher item to download
     */
    public void setGopherItem(GopherItem gopherItem){
        this.item = gopherItem;
    }

    /**
     * Returns the gopher item
     * 
     * @return
     * Returns the gopher item to download
     */
    public GopherItem getGopherItem(){
        return this.item;
    }

    /**
     * Returns the status of this download
     * 
     * @return
     * The status as DownloadStatus enum
     */
    public DownloadStatus getStatus(){
        return this.status;
    }

    /**
     * Returns the number of bytes loaded
     * 
     * @return
     * The number of bytes loaded as long
     */
    public long getByteCountLoaded(){
        return this.byteCountLoaded;
    }

    @Override
    public void progress(GopherUrl url, long byteCount) {
        /* update the local byte counter  */
        this.byteCountLoaded = byteCount;
    }

    @Override
    public void pageLoaded(GopherPage result) {
        this.status = DownloadStatus.COMPLETED;
        System.out.println("Download completed: " + this.item.getUrlString());

        /* check if file open was requested */
        if(this.openFile){
            try{
                /* use the desktop to open the file */
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(this.fileName));
                }else{
                    /* no desktop support here, report one level up */
                    throw new Exception("Desktop not supported");
                }
            }catch(Exception ex){
                /* output the exception that the file could not be opened */
                System.out.println("Unable to open file after download "
                                + "(" + fileName + "):" + ex.getMessage());
            }
        }
    }

    @Override
    public void pageLoadFailed(GopherError error, GopherUrl url) {
        this.status = DownloadStatus.FAILED;
    }
}