package org.gophie.net;

import java.util.ArrayList;

import org.gophie.net.event.DownloadListEventListener;

public class DownloadList extends ArrayList<DownloadItem> {
    private static final long serialVersionUID = 1L;

    /* event listeners for list updated */
    private ArrayList<DownloadListEventListener> eventListener = new ArrayList<DownloadListEventListener>();

    /**
     * Returns all items as an array
     * 
     * @return
     * All download items as an array
     */
    public DownloadItem[] getDownloadItemArray(){
        DownloadItem[] result = new DownloadItem[this.size()];

        /* create the list upside down with the latest first */
        for(int i=this.size()-1; i>=0; i--){
            result[i] = this.get(i);
        }

        return result;
    }

    public void addEventListener(DownloadListEventListener listener){
        this.eventListener.add(listener);
    }

    private void notifyUpdate(){
        for(DownloadListEventListener listener: this.eventListener){
            listener.downloadListUpdated();
        }
    }

    @Override
    public boolean add(DownloadItem e){
        boolean result = super.add(e);
        this.notifyUpdate();        
        return result;
    }

    @Override
    public void clear(){
        super.clear();
        this.notifyUpdate();   
    }
}