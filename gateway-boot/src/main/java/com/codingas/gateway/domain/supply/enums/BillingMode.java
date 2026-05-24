package com.codingas.gateway.domain.supply.enums;

/**
 * 计费模式
 */
public enum BillingMode {

    /** 按量计费 */
    PAY_AS_YOU_GO("pay_as_you_go", "按量计费"),

    /** 订阅制 Coding Plan */
    SUBSCRIPTION_CODING("subscription_coding", "Coding Plan"),

    /** 订阅制 Token Plan */
    SUBSCRIPTION_TOKEN("subscription_token", "Token Plan");

    private final String code;
    private final String displayName;

    BillingMode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BillingMode fromCode(String code) {
        for (BillingMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown billing mode: " + code);
    }
}