/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.exception;

/**
 * 认证失败异常
 *
 * <p>表示认证过程中验证失败（如 API Key 无效）。</p>
 */
public class AuthenticationFailedException extends IamException {

    private static final String CODE = "AUTHENTICATION_FAILED";

    public AuthenticationFailedException(String message) {
        super(CODE, message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public AuthenticationFailedException() {
        super(CODE, "Authentication failed");
    }
}
