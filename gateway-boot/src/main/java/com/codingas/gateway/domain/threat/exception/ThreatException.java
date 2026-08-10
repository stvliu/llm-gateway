/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.threat.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * Threat 子域根异常
 */
public class ThreatException extends GatewayException {

    public ThreatException(String code, String message) {
        super(code, message);
    }

    public ThreatException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
