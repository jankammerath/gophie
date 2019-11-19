package net;

import java.io.*;
import java.net.Socket;

public class GopherClient {
    public void fetchAsync(String url){
        
    }

    public String fetch(String url){
        String result = "";

        try{
            /* parse the url and instanciate the client */
            GopherUrl gopherUrl = new GopherUrl(url);
            Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());
            (new DataOutputStream(gopherSocket.getOutputStream())).writeBytes("\r\n");
            BufferedReader responseBuffer = new BufferedReader(new InputStreamReader(gopherSocket.getInputStream()));
            
            for (String line = responseBuffer.readLine(); line != null; line = responseBuffer.readLine()) {
                result += line + "\n";
            }

            System.out.println("=== GOPHER PAGE ===");
            System.out.print(result);

            gopherSocket.close();
        }catch(Exception ex){
            /* handle the error properly and raise and event */
            System.out.println("Oopsie, connection failed: " + ex.getMessage());
        }

        return result;
    }
}