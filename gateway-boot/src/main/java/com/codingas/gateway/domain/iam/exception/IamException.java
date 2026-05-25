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
