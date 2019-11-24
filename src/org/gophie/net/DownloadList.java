package org.gophie.net;

import java.util.ArrayList;

public class DownloadList extends ArrayList<DownloadItem> {
    private static final long serialVersionUID = 1L;

    /**
     * Returns all items as an array
     * 
     * @return
     * All download items as an array
     */
    public DownloadItem[] getDownloadItemArray(){
        DownloadItem[] result = new DownloadItem[this.size()];

        for(int i=0; i<this.size(); i++){
            result[i] = this.get(i);
        }

        return result;
    }
}