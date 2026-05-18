package com.codingas.gateway.domain.product.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 产品未找到异常
 */
public class ProductNotFoundException extends GatewayException {

    public ProductNotFoundException(Long productId) {
        super("PRODUCT_NOT_FOUND", "Product not found: id=" + productId);
    }

    public ProductNotFoundException(String message) {
        super("PRODUCT_NOT_FOUND", message);
    }
}
