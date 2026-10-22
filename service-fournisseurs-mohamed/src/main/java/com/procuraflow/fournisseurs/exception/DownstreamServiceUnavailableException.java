package com.procuraflow.fournisseurs.exception;

public class DownstreamServiceUnavailableException extends RuntimeException {

    public DownstreamServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownstreamServiceUnavailableException(String message) {
        super(message);
    }
}
