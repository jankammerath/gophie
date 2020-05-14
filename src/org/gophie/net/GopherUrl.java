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

public class GopherUrl {
    private int port = 70;
    private String host;
    private String selector;

    /**
     * Returns the port number of this address
     * 
     * @return
     * the port number as integer
     */
    public int getPort(){
        return this.port;
    }

    /**
     * Returns the host name of this url
     * 
     * @return
     * host name as string
     */
    public String getHost(){
        return this.host;
    }

    /**
     * Returns the selector of this url
     * 
     * @return
     * the selector as string
     */
    public String getSelector(){
        return this.selector;
    }

    /**
     * Returns the type prefix from the url, if any
     * 
     * @return
     * type prefix (gopher item type code) as string
     */
    public String getTypePrefix(){
        String result = null;

        if(this.selector.length() >= 3){
            if(this.selector.charAt(0) == '/' && this.selector.charAt(2) == '/'){
                String itemTypeCode = this.selector.substring(1,2);
                if(itemTypeCode.matches("[0-9\\+gIThis\\?]")){
                    result = itemTypeCode;
                }
            }
        }

        return result;
    }

    /**
     * Determines whether the url's selector 
     * has a type prefix for the gopher item
     * @return
     */
    public boolean hasTypePrefix(){
        boolean result = false;

        if(this.getTypePrefix() != null){
            result = true;
        }

        return result;
    }

    /**
     * Returns the url string for this url
     * 
     * @return
     * the url as string
     */
    public String getUrlString(){
        String result = this.host;

        if(this.port != 70){
            result += ":" + this.port;
        }

        /* strip the item type prefix as it is just
            for presentation and technically not part
            of the url itself */
        String selectorValue = this.selector;
        if(this.hasTypePrefix()){
            selectorValue = selectorValue.substring(3);
        }

        if(this.selector.length() > 0){
            if(this.selector.startsWith("/")){
                result += selectorValue;
            }else{
                result += "/" + selectorValue;
            }
        }

        return result;
    }
    
    /**
     * constructs the object and parses the url
     * 
     * @param url
     * the url to parse as string
     */
    public GopherUrl(String url){
        this.host = url;

        /* check if the url contains the protocol specifier */
        if(this.host.startsWith("gopher://") == true){
            this.host = this.host.substring(9);
        }

        /* check if a selector was provided */
        if(this.host.indexOf("/") > 0){
            this.selector = this.host.substring(this.host.indexOf("/"));
            this.host = this.host.substring(0, this.host.indexOf("/"));
        }else{
            /* no selector present, set the default to empty string */
            this.selector = "";
        }

        /* check if a port number was provided */
        if(this.host.indexOf(":") > 0){
            /* remove port number from host name */
            String[] valueList = this.host.split(":");
            this.host = valueList[0];

            /* set the port number separately */
            this.port = Integer.parseInt(valueList[1]);
        }
    }
}