package com.adoptar.exception;

public class TelAlreadyExistsException extends RuntimeException {
    public TelAlreadyExistsException(String message) {
        super(message);
    }
}
