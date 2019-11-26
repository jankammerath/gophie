/*
    This file is part of Gophie.

    Gophie is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gophie is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gophie. If not, see <https://www.gnu.org/licenses/>.

*/

package org.gophie.net;

import java.util.ArrayList;

import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.net.event.DownloadItemEventListener;
import org.gophie.net.event.DownloadListEventListener;

public class DownloadList extends ArrayList<DownloadItem> implements DownloadItemEventListener {
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
            result[(this.size()-1)-i] = this.get(i);
        }

        return result;
    }

    public Boolean hasNonActiveItems(){
        Boolean result = false;

        for(DownloadItem item: this){
            if(item.getStatus() != DownloadStatus.ACTIVE){
                result = true;
            }
        }

        return result;
    }

    public void clearNonActiveItems(){
        ArrayList<DownloadItem> currentList = this;
        this.clear();

        for(DownloadItem item: currentList){
            if(item.getStatus() == DownloadStatus.ACTIVE){
                this.add(item);
            }
        }

        this.notifyUpdate();
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
        e.addEventListener(this);
        this.notifyUpdate();        
        return result;
    }

    @Override
    public void clear(){
        super.clear();
        this.notifyUpdate();   
    }

    @Override
    public void downloadProgressReported() {
        for(DownloadListEventListener listener: this.eventListener){
            listener.downloadProgressReported();
        }       
    }
}