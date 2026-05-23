package com.codingas.gateway.domain.metadata.enums;

/**
 * 产品类型枚举
 * <p>
 * 定义供应商提供的产品/套餐类型。
 * </p>
 */
public enum ProductType {

    /** 标准按量付费 */
    STANDARD("standard", "按量付费"),

    /** 批量异步（通常50%折扣） */
    BATCH("batch", "批量处理"),

    /** 缓存折扣 */
    CACHE("cache", "缓存折扣"),

    /** 订阅制（Coding Plan、Token Plan） */
    SUBSCRIPTION("subscription", "订阅制"),

    /** 限时优惠 */
    PROMOTION("promotion", "限时优惠"),

    /** 免费额度 */
    FREE_TIER("free_tier", "免费额度");

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
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown product type: " + code);
    }
}
