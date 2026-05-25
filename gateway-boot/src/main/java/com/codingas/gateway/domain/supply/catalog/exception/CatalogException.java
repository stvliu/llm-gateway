package com.codingas.gateway.domain.supply.catalog.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 目录领域异常
 */
public class CatalogException extends GatewayException {

    public CatalogException(String code, String message) {
        super(code, message);
    }

    public CatalogException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
