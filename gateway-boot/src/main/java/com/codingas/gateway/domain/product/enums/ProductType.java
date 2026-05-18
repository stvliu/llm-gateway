package com.codingas.gateway.domain.product.enums;

/**
 * 产品类型枚举
 *
 * <p>定义供应商提供的产品计费类型。</p>
 */
public enum ProductType {

    /** 按量计费产品 */
    PAY_AS_YOU_GO("pay_as_you_go", "按量计费"),

    /** 订阅制 Coding Plan */
    SUBSCRIPTION_CODING("subscription_coding", "Coding Plan"),

    /** 订阅制 Token Plan */
    SUBSCRIPTION_TOKEN("subscription_token", "Token Plan");

    private final String code;
    private final String displayName;

    ProductType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProductType fromCode(String code) {
        for (ProductType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown product type: " + code);
    }
}
