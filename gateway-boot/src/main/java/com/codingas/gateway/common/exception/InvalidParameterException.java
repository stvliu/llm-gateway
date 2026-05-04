package com.codingas.gateway.common.exception;

public class InvalidParameterException extends RuntimeException {
    private final String field;

    public InvalidParameterException(String field, String message) {
        super(String.format("Invalid parameter %s: %s", field, message));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
