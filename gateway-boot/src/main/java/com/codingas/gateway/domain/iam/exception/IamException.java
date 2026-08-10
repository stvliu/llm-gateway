/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * IAM 子域根异常
 */
public class IamException extends GatewayException {

    public IamException(String code, String message) {
        super(code, message);
    }

    public IamException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
