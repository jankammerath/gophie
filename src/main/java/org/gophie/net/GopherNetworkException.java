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

import org.gophie.net.event.*;

public class GopherNetworkException extends Exception {
    private static final long serialVersionUID = 1L;

    private GopherError errorCode;
    private String errorMessage;

    public GopherNetworkException(GopherError errorTypeCode, String errorTypeMessage){
        this.errorCode = errorTypeCode;
        this.errorMessage = errorTypeMessage;
    }

    public String getGopherErrorMessage(){
        return this.errorMessage;
    }

    public GopherError getGopherErrorType(){
        return this.errorCode;
    }
}