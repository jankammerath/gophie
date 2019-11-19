package net;

import java.io.IOException;
import java.nio.channels.*;

public class GopherClient {
    private AsynchronousSocketChannel client;

    public void fetch(String url){
        try{
            /* parse the url */
            GopherUrl gopherUrl = new GopherUrl(url);
            System.out.println("Server Host: " + gopherUrl.getHost());
            System.out.println("Server Port: " + gopherUrl.getPort());
            System.out.println("Selector: " + gopherUrl.getSelector());

            this.client = AsynchronousSocketChannel.open();
            // InetSocketAddress hostAddress = new InetSocketAddress("localhost", 4999);
        }catch(IOException ex){
            /* handle the error properly and raise and event */
            System.out.println("Oopsie, connection failed: " + ex.getMessage());
        }
    }
}