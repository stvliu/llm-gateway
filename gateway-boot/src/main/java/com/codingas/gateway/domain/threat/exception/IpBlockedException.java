/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.threat.exception;

/**
 * IP 封禁异常
 *
 * <p>表示请求来源 IP 被封禁。</p>
 */
public class IpBlockedException extends ThreatException {

    private static final String CODE = "IP_BLOCKED";

    public IpBlockedException(String message) {
        super(CODE, message);
    }

    public IpBlockedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public IpBlockedException() {
        super(CODE, "IP is blocked");
    }
}
