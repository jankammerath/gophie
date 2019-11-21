package net;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import net.GopherItem.GopherItemType;
import net.event.*;

public class GopherClient {
    /* thread with the active fetch process */
    private Thread thread;

    /**
     * Cancels a current fetch operation
     */
    public void cancelFetch(){
        if(this.thread != null){
            this.thread.interrupt();
        }
    }

    /* 
        Fetches a gopher page asynchronously

        @url                the url of the gopher page to fetch
        @eventListener      the listener to report the result to
    */
    public void fetchAsync(String url, GopherItemType contentType, GopherClientEventListener eventListener){
        /* instanciate the new thread */
        this.thread = new Thread(new Runnable() { 
            public void run() { 
                try{
                    GopherPage resultPage = fetch(url, contentType);
                    if (eventListener != null) { 
                        eventListener.pageLoaded(resultPage); 
                    } 
                }catch(GopherNetworkException ex){
                    if (eventListener != null) { 
                        eventListener.pageLoadFailed(ex.getGopherErrorType());
                    } 
                }
            } 
        });

        /* start the new thread */
        this.thread.start();
    }

    /*
        Fetches a gopher page

        @url                the url of the gopher page to fetch
    */
    public GopherPage fetch(String url, GopherItemType contentType) throws GopherNetworkException {
        GopherPage result = null;

        try{
            /* string result with content */
            byte[] content;

            System.out.println("Fetching (" + contentType.toString() + "): " + url);

            /* parse the url and instanciate the client */
            GopherUrl gopherUrl = new GopherUrl(url);
            Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());
            (new DataOutputStream(gopherSocket.getOutputStream())).writeBytes(gopherUrl.getSelector() + "\r\n");
            content = gopherSocket.getInputStream().readAllBytes();

            /* close the socket to the server */
            gopherSocket.close();

            /* set the result page */
            result = new GopherPage(content, contentType, gopherUrl);
        }catch(UnknownHostException ex){
            /* handle host not found exception */
            throw new GopherNetworkException(GopherError.HOST_UNKNOWN, ex.getMessage());
        }catch(SocketTimeoutException ex){
            /* handle host not found exception */
            throw new GopherNetworkException(GopherError.CONNECTION_TIMEOUT, ex.getMessage());
        }catch(Exception ex){
            /* handle the error properly and raise and event */
            System.out.println("GOPHER NETWORK EXCEPTION: " + ex.getMessage());
            ex.printStackTrace();
            throw new GopherNetworkException(GopherError.EXCEPTION, ex.getMessage());
        }

        return result;
    }
}