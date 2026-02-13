package com.walletService.Exceptions;

public class UnauthorizedWalletAccessException extends RuntimeException {

    public UnauthorizedWalletAccessException(String message) {
        super(message);
    }

    public UnauthorizedWalletAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
