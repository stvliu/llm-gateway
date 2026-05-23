package com.codingas.gateway.domain.dataprotection.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * DataProtection 子域根异常
 */
public class DataProtectionException extends GatewayException {

    public DataProtectionException(String code, String message) {
        super(code, message);
    }

    public DataProtectionException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
