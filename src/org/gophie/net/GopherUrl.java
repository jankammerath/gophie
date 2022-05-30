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
    private String proto = "gopher";
    private int port = 0;
    private String host;
    private String selector;


    /**
     * Return the URL's protocol.
     */
    public String getProto() {
        return proto;
    }

    /**
     * Returns the default port for the protocol.
     */
    public int getDefaultPort() {
        return proto.equals("http")? 80: 70;
    }

    /**
     * Returns the port number of this address
     * 
     * @return
     * the port number as integer
     */
    public int getPort(){
        return this.port == 0? getDefaultPort(): this.port;
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
     * without type prefix, if present
     * 
     * @return
     * the url string of the url
     */
    public String getUrlString(){
        return this.getUrlString(false);
    }

    /**
     * Combines the possible relative URL url and
     * returns the result; http logic is applied.
     */
    public String makeUrlString(String url) {

        /* check if url is already complete */
        if (url.indexOf("://") >= 0)
	    return url;

        String u2 = this.getProto() + "://" + this.getHost();
        if (this.getPort() != this.getDefaultPort())
            u2 = u2 + ":" + this.getPort();
    
        if (url.startsWith("/"))
            u2 = u2 + url;                     /* absolute path. */
        else {
            String p = this.getSelector();     /* relative path. */
            if (this.hasTypePrefix())
                p = p.substring(2);

            int k = p.lastIndexOf("/");
            if (k >= 0)
                p = p.substring(0, k + 1);
    
            u2 = u2 + p + url;
	    }

	return u2;
    }

    /**
     * Sets or overwrites the type prefix for this url
     * 
     * @param prefix
     * single-character type prefix as string
     */
    public void setTypePrefix(String prefix){
        /* check if a type prefix is present already */
        if(this.hasTypePrefix()){
            /* replace the existing type prefix with the new one */
            if(this.selector.length() > 3){
                if(this.selector.substring(3,4).equals("/")){
                    this.selector = "/" + prefix + this.selector.substring(3);
                }else{
                    this.selector = "/" + prefix + "/" + this.selector.substring(3);
                }
            }else{
                /* only the prefix is in the selector, replace it */
                this.selector = "/" + prefix + "/";
            }
        }else{
            if(this.selector.length() > 0){
                /* just add the type prefix to the selector */
                if(this.selector.substring(0,1).equals("/")){
                    this.selector = "/" + prefix + this.selector;
                }else{
                    this.selector = "/" + prefix + "/" + this.selector;
                }
            }else{
                /* just set the prefix as the selector */
                this.selector = "/" + prefix + "/";
            }
        }
    }

    /**
     * Returns the url string for this url
     * 
     * @param includeTypePrefix
     * when true will include the type prefix in the 
     * url, if any is available. If none is available, 
     * this parameter has no effect.
     * 
     * @return
     * the url as string
     */
    public String getUrlString(boolean includeTypePrefix){
        String result = this.host;

        if(this.port != getDefaultPort()){
            result += ":" + this.port;
        }

        /* strip the item type prefix as it is just
            for presentation and technically not part
            of the url itself */
        String selectorValue = this.selector;
        if(this.hasTypePrefix()){
            if(!includeTypePrefix){
                /* remove the type prefix if requested */
                selectorValue = selectorValue.substring(3);
            }
        }

        if(selectorValue.length() > 0){
            if(selectorValue.startsWith("/")){
                result += selectorValue;
            }else{
                result += "/" + selectorValue;
            }
        }

	if (proto.equals("http"))
		result = "http://" + result;

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
	    this.proto = "gopher";
        } else if (this.host.startsWith("http://")) {
	    this.host = this.host.substring(7);
	    this.proto = "http";
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
