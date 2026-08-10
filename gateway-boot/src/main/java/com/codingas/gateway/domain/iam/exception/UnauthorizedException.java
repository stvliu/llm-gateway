/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.exception;

/**
 * 未认证异常
 *
 * <p>表示请求缺少有效的认证信息。</p>
 */
public class UnauthorizedException extends IamException {

    private static final String CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(CODE, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public UnauthorizedException() {
        super(CODE, "Authentication required");
    }
}
