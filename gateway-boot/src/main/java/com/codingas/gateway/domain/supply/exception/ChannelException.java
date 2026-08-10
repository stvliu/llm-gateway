/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 渠道异常
 */
public class ChannelException extends GatewayException {

    public ChannelException(String code, String message) {
        super(code, message);
    }

    public ChannelException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}