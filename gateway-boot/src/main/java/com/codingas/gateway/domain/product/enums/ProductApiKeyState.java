package com.codingas.gateway.domain.product.enums;

/**
 * 产品 API Key 状态枚举
 */
public enum ProductApiKeyState {

    /** 活跃状态 */
    ACTIVE("active"),

    /** 已停用 */
    INACTIVE("inactive"),

    /** 已删除 */
    DELETED("deleted");

    private final String code;

    ProductApiKeyState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static ProductApiKeyState fromCode(String code) {
        for (ProductApiKeyState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown product api key state: " + code);
    }
}
