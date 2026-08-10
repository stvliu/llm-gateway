/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
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
