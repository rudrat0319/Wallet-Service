package com.walletService.Exceptions;

public class InvalidAssetTypeException extends RuntimeException {

    public InvalidAssetTypeException(String message) {
        super(message);
    }

    public InvalidAssetTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
