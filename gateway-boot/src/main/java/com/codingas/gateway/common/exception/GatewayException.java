/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.common.exception;

/**
 * 网关异常根类
 *
 * <p>所有网关异常的基类。</p>
 */
public class GatewayException extends RuntimeException {

    private final String code;

    public GatewayException(String code, String message) {
        super(message);
        this.code = code;
    }

    public GatewayException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
