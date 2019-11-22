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