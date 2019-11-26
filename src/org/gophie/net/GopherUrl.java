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

    public int getPort(){
        return this.port;
    }

    public String getHost(){
        return this.host;
    }

    public String getSelector(){
        return this.selector;
    }

    public String getUrlString(){
        String result = this.host;

        if(this.port != 70){
            result += ":" + this.port;
        }

        if(this.selector.length() > 0){
            if(this.selector.startsWith("/")){
                result += this.selector;
            }else{
                result += "/" + this.selector;
            }
        }

        return result;
    }
    
    /* constructs the object and parses the url */
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