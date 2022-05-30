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

import org.gophie.io.FileSignature;
import org.gophie.io.FileSignature.FileSignatureType;
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
		    boolean haveHttpHeader = false;

                    /* create the output file stream to write to */
                    OutputStream fileStream = new FileOutputStream(new File(targetFile));

                    /* parse the url and instanciate the client */
                    GopherUrl gopherUrl = new GopherUrl(url);
                    Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());

		    if (gopherUrl.getProto().equals("http")) {
			byte[] httpRequest = ("GET " + gopherUrl.getSelector() + " HTTP/1.0\r\n" +
					"Host: " + gopherUrl.getHost() +"\r\n" +
					"\r\n").getBytes(StandardCharsets.US_ASCII);
			(new DataOutputStream(gopherSocket.getOutputStream())).write(httpRequest);
			haveHttpHeader = true;
			}
		    else {
			byte[] gopherRequest = (gopherUrl.getSelector() + "\r\n").getBytes(StandardCharsets.US_ASCII);
			(new DataOutputStream(gopherSocket.getOutputStream())).write(gopherRequest);
			}

                    /* read byte in chunks and report progress */
                    int read;
                    InputStream socketStream = gopherSocket.getInputStream();
                    byte[] data = new byte[16384];
                    long totalByteCount = 0;

                    /* read byte by byte to be able to report progress */
                    while ((read = socketStream.read(data, 0, data.length)) != -1) {
			if (haveHttpHeader) {

			    /* Look for the end of the HTTP header `\n\r?\n`
			     * and delete the header from the data.  This is
			     * just a dirty hack. */ 
			    for (int i = 1; i < read; i++) {
				if (data[i] == '\n') {
				    if (data[i-1] == '\n'  ||
					    (i >= 2  &&  data[i-2] == '\n'  &&  data[i-1] == '\r')) {
					i++;
					System.arraycopy(data, i, data, 0, read - i);
					read -= i;
					haveHttpHeader = false;
					break;
				    }				
				}
			    }
			}

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
                }catch(GopherItemTypeException ex){
                    if(!clientObject.isCancelled()){
                        if (eventListener != null) { 
                            eventListener.pageLoadItemMismatch(ex.getRequestedType(),ex.getDetectedType(),new GopherUrl(url));
                        } 
                    }
                }
            } 
        });

        /* start the new thread */
        this.thread.start();
    }

    /**
     * Fetches a gopher page. Do not use this method for fetching
     * anything other than gopher menus or pages, text files or images
     * as this method will throw an exception if it encounters media 
     * files or other binary data.
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
    public GopherPage fetch(String url, GopherItemType contentType, GopherClientEventListener eventListener) throws GopherNetworkException, GopherItemTypeException {
        GopherPage result = null;

        try{
	    /* for HTTP requests */
	    boolean haveHttpHeader = false;

            /* reset the cancellation indicator */
            this.cancelled = false;

            /* string result with content */
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            /* parse the url and instanciate the client */
            GopherUrl gopherUrl = new GopherUrl(url);
            Socket gopherSocket = new Socket(gopherUrl.getHost(), gopherUrl.getPort());

	    if (gopherUrl.getProto().equals("http")) {
                byte[] httpRequest = ("GET " + gopherUrl.getSelector() + " HTTP/1.0\r\n" +
				"Host: " + gopherUrl.getHost() +"\r\n" +
				"\r\n").getBytes(StandardCharsets.US_ASCII);
                (new DataOutputStream(gopherSocket.getOutputStream())).write(httpRequest);
		haveHttpHeader = true;
	    	}
	    else {
                byte[] gopherRequest = (gopherUrl.getSelector() + "\r\n").getBytes(StandardCharsets.US_ASCII);
                (new DataOutputStream(gopherSocket.getOutputStream())).write(gopherRequest);
		}

            /* read byte in chunks and report progress */
            int read;
            InputStream socketStream = gopherSocket.getInputStream();
            byte[] data = new byte[16384];
            long totalByteCount = 0;

            /* read byte by byte to be able to report progress */
            while ((read = socketStream.read(data, 0, data.length)) != -1) {
	        if (haveHttpHeader) {

                    /* Look for the end of the HTTP header `\n\r?\n`
		     * and delete the header from the data.  This is
		     * just a dirty hack. */ 
		    for (int i = 1; i < read; i++) {
		    	if (data[i] == '\n') {
			    if (data[i-1] == '\n'  ||
			            (i >= 2  &&  data[i-2] == '\n'  &&  data[i-1] == '\r')) {
				i++;
		                System.arraycopy(data, i, data, 0, read - i);
				read -= i;
				haveHttpHeader = false;
				break;
                            }				
		        }
		    }
                }

                /* check the file signature from the first bytes received */
                if(totalByteCount == 0){
                    FileSignature fileSignature = new FileSignature(data);
                    FileSignatureType fileType = fileSignature.getSignatureItemType();

                    /* check if the actual file type is an image */
                    if(fileType == FileSignatureType.IMAGE && 
                        (contentType != GopherItemType.IMAGE_FILE 
                        || contentType != GopherItemType.GIF_FILE)){
                        /* when the detected file signature is an image, but
                            the original gopher item type defined is neither
                            a generic image nor a gif file, simply fix the
                            item type by setting it to an image */
                        contentType = GopherItemType.IMAGE_FILE;
                    }

                    /* check if the actual file is a media file */
                    if(fileType == FileSignatureType.MEDIA){
                        /* fetching media files needs to be done
                            through the download method. this method
                            is for fetching gopher pages, text and images */
                        throw new GopherItemTypeException(url, contentType, GopherItemType.SOUND_FILE);
                    }if(fileType == FileSignatureType.BINARY){
                        /* same goes for binary files */
                        throw new GopherItemTypeException(url, contentType, GopherItemType.BINARY_FILE);
                    }
                }

                /* verify that the provided file is actually a text file
                    as it seems to be getting very big and might be a 
                    binary or media file */
                if((contentType == GopherItemType.GOPHERMENU 
                    || contentType == GopherItemType.TEXTFILE
                    || contentType == GopherItemType.UNKNOWN)
                    && totalByteCount > 200000){
                    /* check if the data is text content or not */
                    FileSignature largeSignature = new FileSignature(data);
                    FileSignatureType largeType = largeSignature.getSignatureItemType();

                    /* throw an exception when this file does not match */
                    if(largeType != FileSignatureType.TEXT){
                        /* throw the item type exception and define this as a generic binary */
                        throw new GopherItemTypeException(url, contentType, GopherItemType.BINARY_FILE);
                    }
                }

                /* write the data to the buffer */
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
        }catch(GopherItemTypeException ex){
            /* just pass through the item type exception */
            throw ex;
        }catch(Exception ex){
            /* handle the error properly and raise and event */
            System.out.println("GOPHER NETWORK EXCEPTION: " + ex.getMessage());
            throw new GopherNetworkException(GopherError.EXCEPTION, ex.getMessage());
        }

        return result;
    }
}
