package com.codingas.gateway.domain.threat.exception;

import com.codingas.gateway.domain.threat.exception.ThreatException;

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
