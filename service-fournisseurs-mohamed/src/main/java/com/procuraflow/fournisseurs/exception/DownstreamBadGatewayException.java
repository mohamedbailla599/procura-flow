package com.procuraflow.fournisseurs.exception;

public class DownstreamBadGatewayException extends RuntimeException {

    public DownstreamBadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownstreamBadGatewayException(String message) {
        super(message);
    }
}
