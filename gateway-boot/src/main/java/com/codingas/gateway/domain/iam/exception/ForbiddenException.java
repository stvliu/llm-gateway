/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.exception;

/**
 * 无权限异常
 *
 * <p>表示用户已认证但无权访问资源。</p>
 */
public class ForbiddenException extends IamException {

    private static final String CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(CODE, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public ForbiddenException() {
        super(CODE, "Access denied");
    }
}
