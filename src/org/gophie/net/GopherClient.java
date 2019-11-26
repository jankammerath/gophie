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

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.event.*;

public class GopherClient {
    /* thread with the active fetch process */
    private Thread thread;
    private Boolean cancelled = false;

    /**
     * Cancels a current fetch operation
     */
    public void cancelFetch(){
        if(this.thread != null){
            this.thread.interrupt();
            this.cancelled = true;
        }
    }

    /**
     * Returns whether the current 
     * operation was cancelled or not
     * 
     * @return
     * true when cancelled, false otherwise
     */
    public Boolean isCancelled(){
        return this.cancelled;
    }

    /**
     * Downloads content through gopher and 
     * stores it in the define target file
     * 
     * @param url
     * Url to download the content from
     * 
     * @param targetFile
     * The file to write the content to
     * 
     * @param eventListener
     * Listener to report the status to
     */
    public void downloadAsync(String url, String targetFile, GopherClientEventListener eventListener){
        /* instanciate the new thread */
        GopherClient clientObject = this;
        this.thread = new Thread(new Runnable() { 
            public void run() { 
                try{
                    /* create the output file stream to write to */
                    OutputStream fileStream = new FileOutputStream(new File(targetFile));

                    /* parse the url and instanciate the client */
                    GopherUrl gopherUrl = new GopherUrl(url);
                    Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());
                    byte[] gopherRequest = (gopherUrl.getSelector() + "\r\n").getBytes(StandardCharsets.US_ASCII);
                    (new DataOutputStream(gopherSocket.getOutputStream())).write(gopherRequest);

                    /* read byte in chunks and report progress */
                    int read;
                    InputStream socketStream = gopherSocket.getInputStream();
                    byte[] data = new byte[16384];
                    long totalByteCount = 0;

                    /* read byte by byte to be able to report progress */
                    while ((read = socketStream.read(data, 0, data.length)) != -1) {
                        fileStream.write(data, 0, read);

                        /* calculate total bytes read */
                        totalByteCount = totalByteCount + data.length;

                        /* report byte count to listener */
                        if(!clientObject.isCancelled()){
                            if(eventListener != null){
                                eventListener.progress(gopherUrl, totalByteCount);
                            }
                        }
                    }

                    /* close the socket to the server */
                    gopherSocket.close();

                    /* close the file stream */
                    fileStream.close();

                    if(!clientObject.isCancelled()){
                        if (eventListener != null) { 
                            eventListener.pageLoaded(null); 
                        } 
                    }
                }catch(Exception ex){
                    /* log the exception message */
                    System.out.println("Download failed (" + url + "):" + ex.getMessage());

                    /* remove the file if already created */
                    File createdFile = new File(targetFile);
                    if(createdFile.exists()){ createdFile.delete(); }

                    /* notify the handlers */
                    if(!clientObject.isCancelled()){
                        if (eventListener != null) { 
                            eventListener.pageLoadFailed(GopherError.EXCEPTION,new GopherUrl(url));
                        } 
                    }
                }
            } 
        });

        /* start the new thread */
        this.thread.start();        
    }

    /**
     * Fetches a gopher page asynchronously
     * 
     * @param url
     * the url of the gopher page to fetch
     * 
     * @param contentType
     * the expected content type of the url
     * 
     * @param eventListener
     * the listener to report the result to
     */
    public void fetchAsync(String url, GopherItemType contentType, GopherClientEventListener eventListener){
        /* instanciate the new thread */
        GopherClient clientObject = this;
        this.thread = new Thread(new Runnable() { 
            public void run() { 
                try{
                    GopherPage resultPage = fetch(url, contentType, eventListener);

                    if(!clientObject.isCancelled()){
                        if (eventListener != null) { 
                            eventListener.pageLoaded(resultPage); 
                        } 
                    }
                }catch(GopherNetworkException ex){
                    if(!clientObject.isCancelled()){
                        if (eventListener != null) { 
                            eventListener.pageLoadFailed(ex.getGopherErrorType(),new GopherUrl(url));
                        } 
                    }
                }
            } 
        });

        /* start the new thread */
        this.thread.start();
    }

    /**
     * Fetches a gopher page
     * 
     * @param url
     * the url of the page to fetch
     * 
     * @param contentType
     * the expected content type
     * 
     * @param eventListener
     * event listener to report progress to
     * 
     * @return
     * the fetched gopher page object
     * 
     * @throws GopherNetworkException
     * Exception with network information
     */
    public GopherPage fetch(String url, GopherItemType contentType, GopherClientEventListener eventListener) throws GopherNetworkException {
        GopherPage result = null;

        try{
            /* string result with content */
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            /* parse the url and instanciate the client */
            GopherUrl gopherUrl = new GopherUrl(url);
            Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());
            byte[] gopherRequest = (gopherUrl.getSelector() + "\r\n").getBytes(StandardCharsets.US_ASCII);
            (new DataOutputStream(gopherSocket.getOutputStream())).write(gopherRequest);

            /* read byte in chunks and report progress */
            int read;
            InputStream socketStream = gopherSocket.getInputStream();
            byte[] data = new byte[16384];
            long totalByteCount = 0;

            /* read byte by byte to be able to report progress */
            while ((read = socketStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);

                /* calculate total bytes read */
                totalByteCount = totalByteCount + data.length;

                /* report byte count to listener */
                if(!this.isCancelled()){
                    if(eventListener != null){
                        eventListener.progress(gopherUrl, totalByteCount);
                    }
                }
            }

            /* close the socket to the server */
            gopherSocket.close();

            /* set the result page */
            result = new GopherPage(buffer.toByteArray(), contentType, gopherUrl);
        }catch(ConnectException ex){
            /* handle host connection errors */
            throw new GopherNetworkException(GopherError.CONNECT_FAILED, ex.getMessage());
        }catch(UnknownHostException ex){
            /* handle host not found exception */
            throw new GopherNetworkException(GopherError.HOST_UNKNOWN, ex.getMessage());
        }catch(SocketTimeoutException ex){
            /* handle host not found exception */
            throw new GopherNetworkException(GopherError.CONNECTION_TIMEOUT, ex.getMessage());
        }catch(Exception ex){
            /* handle the error properly and raise and event */
            System.out.println("GOPHER NETWORK EXCEPTION: " + ex.getMessage());
            throw new GopherNetworkException(GopherError.EXCEPTION, ex.getMessage());
        }

        return result;
    }
}