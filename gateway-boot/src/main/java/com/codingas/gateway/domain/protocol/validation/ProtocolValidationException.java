package com.codingas.gateway.domain.protocol.validation;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 协议校验异常
 */
public class ProtocolValidationException extends GatewayException {

    private static final String ERROR_CODE = "PROTOCOL_VALIDATION_ERROR";

    private final String protocol;
    private final String field;
    private final String violation;

    public ProtocolValidationException(String protocol, String field, String violation) {
        super(ERROR_CODE, String.format("协议校验失败 [%s]: 字段 '%s' %s", protocol, field, violation));
        this.protocol = protocol;
        this.field = field;
        this.violation = violation;
    }

    public String getProtocol() { return protocol; }
    public String getField() { return field; }
    public String getViolation() { return violation; }
}
