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
