/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.common.exception;

public class DuplicateResourceException extends RuntimeException {
    private final String resourceType;
    private final String field;

    public DuplicateResourceException(String resourceType, String field) {
        super(String.format("%s already exists with %s", resourceType, field));
        this.resourceType = resourceType;
        this.field = field;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getField() {
        return field;
    }
}
