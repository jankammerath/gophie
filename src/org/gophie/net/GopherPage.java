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

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;

import org.gophie.config.ConfigurationManager;
import org.gophie.net.GopherItem.GopherItemType;

/**
 * A GopherMenu page object that contains all information
 * and Gopher items provided in the underlying Gopher Menu
 */
public class GopherPage {
    /* defines the default charset */
    private static final String GOPHERPAGE_DEFAULT_CHARSET = "UTF-8";

    /* local variables */
    private byte[] sourceCode;
    private GopherUrl url;
    private ArrayList<GopherItem> itemList;
    private GopherItemType contentType = GopherItemType.UNKNOWN;

    /**
     * Constructs the GopherPage object and if it is 
     * a gopher menu or unknown it tries to parse it
     * as a gopher menu. If that fails, it will try
     * to evaluate the content type and store the 
     * source code.
     * 
     * @param gopherPageSourceCode
     * Source code or content of the gopher page
     * 
     * @param gopherContentType
     * The estimated content type of the gopher page
     * 
     * @param gopherPageUrl
     * The URL of the gopher page
     */
    public GopherPage(byte[] gopherPageSourceCode, GopherItemType gopherContentType, GopherUrl gopherPageUrl){
        this.sourceCode = gopherPageSourceCode;
        this.url = gopherPageUrl;
        this.itemList = new ArrayList<GopherItem>();

        if(gopherContentType == GopherItemType.GOPHERMENU 
            || gopherContentType == GopherItemType.UNKNOWN){
            /* try to parse it as a gopher menu */
            try{
                /* execute the parse process */
                this.parse();

                /* parsing succeeded, define as gopher menu */
                this.contentType = GopherItemType.GOPHERMENU;
            }catch(Exception ex){

                /* Print an exception only if we expected a directory. */
	        if (gopherContentType != GopherItemType.UNKNOWN) {
                    /* output the parser exception */
                    System.out.println("Failed to parse gophermenu: " + ex.getMessage());
                    ex.printStackTrace();
                    }

                /* parsing failed for whatever, define as text */
                this.contentType = GopherItemType.TEXTFILE;
            }
        }else{
            /* set the supplied content type */
            this.contentType = gopherContentType;
        }
    }

    /**
     * Returns the content type of this page
     * 
     * @return
     * The content type as gopher item type
     */
    public GopherItemType getContentType(){
        return this.contentType;
    }

    /**
     * Returns the source code in base64 encoded format
     * which can be used to display images in the view
     * 
     * @return
     * String with base64 encoded data of the source code
     */
    public String getBase64(){
        return Base64.getEncoder().encodeToString(this.sourceCode); 
    }

    /**
     * Returns the raw bytes of the data received
     * 
     * @return
     * Byte array with the raw gopher page data
     */
    public byte[] getByteArray(){
        return this.sourceCode;
    }

    /**
     * Sets the source code (gophermap) of this gopher page
     * 
     * @param value
     * The text value as supplied by the server
     */
    public void setSourceCode(byte[] value){
        this.sourceCode = value;
    }

    /**
     * Returns the source code (gophermap) of this page
     * 
     * @return
     * The gophermap content as a String
     */
    public String getSourceCode(){
        try{
            return new String(this.sourceCode, ConfigurationManager.getConfigFile()
                .getSetting("DEFAULT_CHARSET", "Network", GOPHERPAGE_DEFAULT_CHARSET));
        }catch(Exception ex){
            /* drop a quick info on the console when decoding fails */
            System.out.println("Failed to decode bytes of Gopher Page: " + ex.getMessage());
            return "";
        }
    }

    /**
     * Returns the GopherUrl object for this page
     * 
     * @return
     * GopherUrl object with url of this page
     */
    public GopherUrl getUrl(){
        return this.url;
    }

    /**
     * Returns an array list with all gopher items of this page
     * 
     * @return
     * ArrayList with all GopherItem objects
     */
    public ArrayList<GopherItem> getItemList(){
        return this.itemList;
    }

    /**
     * Returns all text content of this page
     * 
     * @return
     * All text content of this page as string
     */
    public String getTextContent(){
        String result = "";

        if(this.itemList.size() > 0){
            /* get the actual text from all gopher items */
            for(GopherItem item: this.itemList){
                result += item.getUserDisplayString() + "\n";
            }
        }else{
            /* just return the source code and remove the
                line termination from the gopher server */
            return this.getSourceCode().replace("\r\n.\r\n", "");
        }

        return result;
    }

    /**
     * Saves the contents of this 
     * page to the defined file 
     * 
     * @param fileName
     * The file to store content in
     * 
     * @return
     * truen when successful, otherwise false
     */
    public Boolean saveAsFile(String fileName){
        Boolean result = false;

        try{
            /* store this page content to file */
            FileOutputStream fileOutput = new FileOutputStream(fileName);
            fileOutput.write(this.getByteArray());
            fileOutput.close();
            result = true;
        }catch(Exception ex){
            /* output the exception info when file storage failed */
            System.out.println("Failed to save page as file: " + ex.getMessage());
            result = false;
        }

        return result;
    }

    /**
     * Returns the filename for this page
     * 
     * @return
     * The file name as string with extension
     */
    public String getFileName(){
        String result = this.getUrl().getUrlString();

        /* check if the file has a file name */
        if(result.lastIndexOf("/") > 0){
            /* use the file name provided in the url */
            result = result.substring(result.lastIndexOf("/")+1);
        }else{
            /* just call the file index when none exists */
            result = "index";
        }

        /* check if the file has an extension */
        if(result.lastIndexOf(".") == -1){
            result += "." + GopherItem.getDefaultFileExt(this.getContentType());
        }

        return result;
    }

    /**
     * parses the local source code into components
     */
    private void parse() throws GopherItemTypeException {
        String[] itemSourceList = this.getSourceCode().split("\n");

	/* See, if the directory is a Gopher+ $ listing - only to
	 * discard that stuff during parsing. */
	boolean isGopherPlus = itemSourceList.length > 0  &&  itemSourceList[0].startsWith("+");

        for(int i=0; i<itemSourceList.length; i++){
            String itemSource = itemSourceList[i];

            /* If it's Gopher+ then we take only the +INFO lines. */
            if (isGopherPlus) {
	        if (! itemSource.startsWith("+INFO: "))
	            continue;

                itemSource = itemSource.substring(7);
	    }

            /* Do we have a non-empty line? */
            if (itemSource.length() == 0  ||  itemSource.equals("."))
	        continue;

            /* To prevent texts being recognized as directories
	     * some type characters are forbidden. */
	    char typeChar = itemSource.charAt(0);
            if (typeChar == ' '  ||  typeChar == '\t')
	        throw new GopherItemTypeException(url.getUrlString(), GopherItemType.UNKNOWN, GopherItemType.UNKNOWN);

            /* What is happening here is this: Incomplete Gopher selectors
	     * (i.e. empty server field or server == `.`) get their data
	     * from the directory's own address.  Furthermore, file
	     * selectors without a leading slash are interpreted as
	     * relative to the current directory.  That should mimic
	     * http URL semantics and allow relative addressing. */

	    String[] x = itemSource.split("\t");
	    if (itemSource.startsWith("i")  ||  x.length < 2) {
	        /* make sure that we have 4 fields */
		itemSource = x[0] + "\t\t.\t0";
		}
	    else if (x.length < 3  ||  x[2].equals("")  ||  x[2].equals(".")) {

                if (x[1].startsWith("http://")) {
		    /* The entry is a complete URL.  Just make sure that
		     * it has all four fields. */
		    itemSource = x[0] + "\t" + x[1] + "\t.\t0";
		} else if (url.getProto().equals("http")) {
		    /* Create a full URL as selector. */

		    String u2 = url.makeUrlString(x[1]);
		    itemSource = x[0] + "\t" + u2 + "\t.\t0";

		} else {
	            /* Incomplete directory entry: server misssing. */
		    x[2] = url.getHost();

		    if (x.length < 4  ||  x[3].equals("")  ||  x[3].equals(".")  ||  x[3].equals("0"))
		        x[3] = "70";

		    /* Construct the entry. */
		    itemSource = x[0] + "\t" + x[1] + "\t" + x[2] + "\t" + x[3];
		    }
	        }

            if(itemSource.length() > 0 && itemSource.equals(".") == false){
                this.itemList.add(new GopherItem(itemSource));
            }
        }
    }
}
