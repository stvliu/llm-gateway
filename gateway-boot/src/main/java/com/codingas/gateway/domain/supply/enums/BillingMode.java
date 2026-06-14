package com.codingas.gateway.domain.supply.enums;

import lombok.Getter;

/**
 * 计费模式枚举
 *
 * <p>支持主流大模型供应商的四种计费方式：</p>
 * <ul>
 *   <li><b>PAY_AS_YOU_GO</b>: 按量付费，按 Token 消耗收费，无固定费用</li>
 *   <li><b>SUBSCRIPTION</b>: 订阅制，固定周期支付固定费用，包含一定额度</li>
 *   <li><b>HYBRID</b>: 混合计费，基础费 + 超出部分按量</li>
 *   <li><b>PREPAID_PACKAGE</b>: 资源包，一次性购买固定额度的 Token 包</li>
 * </ul>
 */
@Getter
public enum BillingMode {

    /** 按量付费（Pay-as-you-go），按实际 Token 使用量计费 */
    PAY_AS_YOU_GO("pay_as_you_go", "按量计费"),

    /** 订阅制（Subscription），固定月费含一定额度 */
    SUBSCRIPTION("subscription", "订阅制"),

    /** 混合计费（Hybrid），基础费 + 超出部分按量 */
    HYBRID("hybrid", "混合计费"),

    /** 资源包（Prepaid Package），预购 Token 包 */
    PREPAID_PACKAGE("prepaid_package", "资源包");

    private final String code;
    private final String displayName;

    BillingMode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 根据代码获取计费模式
     *
     * @param code 计费模式代码
     * @return 对应的计费模式枚举
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static BillingMode fromCode(String code) {
        for (BillingMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown billing mode code: " + code);
    }

    /**
     * 从枚举名或代码解析计费模式
     *
     * <p>兼容数据库中可能存在的旧枚举值名（SUBSCRIPTION_CODING、SUBSCRIPTION_TOKEN、
     * HYBRID_BASE_PLUS_USAGE、TIERED_SUBSCRIPTION、PACKAGE），统一映射到新的 4 值枚举。</p>
     *
     * @param value 枚举名或代码
     * @return 对应的计费模式枚举
     */
    public static BillingMode resolve(String value) {
        if (value == null) {
            return PAY_AS_YOU_GO;
        }
        // 先尝试按新枚举名或 code 直接匹配
        for (BillingMode mode : values()) {
            if (mode.name().equals(value) || mode.code.equals(value)) {
                return mode;
            }
        }
        // 兼容旧枚举值映射
        return switch (value) {
            case "SUBSCRIPTION_CODING", "SUBSCRIPTION_TOKEN", "TIERED_SUBSCRIPTION", "PACKAGE" -> SUBSCRIPTION;
            case "HYBRID_BASE_PLUS_USAGE" -> HYBRID;
            default -> throw new IllegalArgumentException("Unknown billing mode: " + value);
        };
    }

    /**
     * 判断是否为订阅制模式
     */
    public boolean isSubscription() {
        return this == SUBSCRIPTION;
    }

    /**
     * 判断是否为按量计费模式
     */
    public boolean isPayAsYouGo() {
        return this == PAY_AS_YOU_GO;
    }

    /**
     * 判断是否包含基础费用（订阅制和混合计费）
     */
    public boolean hasBaseFee() {
        return this == HYBRID || this == SUBSCRIPTION;
    }
}