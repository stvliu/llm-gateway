package com.codingas.gateway.domain.supply.enums;

/**
 * 计费模式枚举
 * 
 * <p>支持主流大模型供应商的多种计费方式：</p>
 * <ul>
 *   <li><b>OpenAI</b>: 纯按量计费，按 Token 消耗收费</li>
 *   <li><b>Anthropic</b>: 订阅制（个人/团队）或混合计费（企业版：基础费+按量）</li>
 *   <li><b>国内厂商</b>: 支持按量、资源包、订阅等多种模式</li>
 * </ul>
 */
public enum BillingMode {

    /** 
     * 按量计费（Pay-as-you-go）
     * <p>适用场景：OpenAI API、DeepSeek、通义千问等</p>
     * <p>特点：按实际 Token 使用量计费，无固定费用</p>
     */
    PAY_AS_YOU_GO("pay_as_you_go", "按量计费"),

    /** 
     * 订阅制 - Coding Plan
     * <p>适用场景：Anthropic Pro/Max 个人订阅</p>
     * <p>特点：固定月费，包含一定额度的 API 调用权限</p>
     */
    SUBSCRIPTION_CODING("subscription_coding", "Coding Plan"),

    /** 
     * 订阅制 - Token Plan
     * <p>适用场景：传统 Token 套餐订阅</p>
     * <p>特点：固定月费，包含固定 Token 额度</p>
     */
    SUBSCRIPTION_TOKEN("subscription_token", "Token Plan"),

    /** 
     * 混合计费 - 基础费 + 按量
     * <p>适用场景：Anthropic Enterprise（2026年起）</p>
     * <p>特点：每用户每月基础费（如 $20）+ 超出部分按 API 零售价计费</p>
     */
    HYBRID_BASE_PLUS_USAGE("hybrid_base_plus_usage", "基础费+按量"),

    /** 
     * 资源包模式
     * <p>适用场景：国内云厂商（阿里云、腾讯云等）</p>
     * <p>特点：预付费购买 Token 包，用完再购</p>
     */
    PREPAID_PACKAGE("prepaid_package", "资源包"),

    /** 
     * 分层订阅制
     * <p>适用场景：Anthropic Team（5-150人团队）</p>
     * <p>特点：按席位定价，不同层级有不同额度（标准席$20/月，高级席$100/月）</p>
     */
    TIERED_SUBSCRIPTION("tiered_subscription", "分层订阅");

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
        throw new IllegalArgumentException("Unknown billing mode: " + code);
    }

    /**
     * 判断是否为订阅制模式
     * 
     * @return true 如果是订阅制模式
     */
    public boolean isSubscription() {
        return this == SUBSCRIPTION_CODING 
            || this == SUBSCRIPTION_TOKEN 
            || this == TIERED_SUBSCRIPTION;
    }

    /**
     * 判断是否为按量计费模式
     * 
     * @return true 如果是按量计费模式
     */
    public boolean isPayAsYouGo() {
        return this == PAY_AS_YOU_GO || this == HYBRID_BASE_PLUS_USAGE;
    }

    /**
     * 判断是否包含基础费用
     * 
     * @return true 如果有固定基础费用
     */
    public boolean hasBaseFee() {
        return this == HYBRID_BASE_PLUS_USAGE || isSubscription();
    }
}
