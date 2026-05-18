package com.codingas.gateway.domain.product.enums;

/**
 * 产品状态枚举
 */
public enum ProductState {

    /** 活跃状态，可正常使用 */
    ACTIVE("active"),

    /** 已停用，暂停服务 */
    INACTIVE("inactive"),

    /** 已删除 */
    DELETED("deleted");

    private final String code;

    ProductState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isAvailable() {
        return this == ACTIVE;
    }

    public static ProductState fromCode(String code) {
        for (ProductState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown product state: " + code);
    }
}
