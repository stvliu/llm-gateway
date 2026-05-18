package com.codingas.gateway.domain.proxy.entity;

import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.product.enums.ProductType;

/**
 * 路由上下文，携带请求路由所需的全部信息
 *
 * <p>支持新旧两种架构：</p>
 * <ul>
 *   <li>旧架构：providerId + providerApiKey + endpoint</li>
 *   <li>新架构：productId + productType + userApiKeyId + teamId</li>
 * </ul>
 */
public class RoutingContext {

    // ===== 旧架构字段（降级兼容） =====
    private Long providerId;
    private String providerName;
    private ProviderType providerType;

    // ===== 新架构字段 =====
    private Long productId;
    private ProductType productType;
    private Long userApiKeyId;
    private Long teamId;

    // ===== 共用字段 =====
    private String model;
    private String protocol;
    private String providerApiKey;
    private Long providerApiKeyId;
    private String endpoint;

    private RoutingContext() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RoutingContext context = new RoutingContext();

        public Builder providerId(Long providerId) { context.providerId = providerId; return this; }
        public Builder providerName(String providerName) { context.providerName = providerName; return this; }
        public Builder providerType(ProviderType providerType) { context.providerType = providerType; return this; }
        public Builder productId(Long productId) { context.productId = productId; return this; }
        public Builder productType(ProductType productType) { context.productType = productType; return this; }
        public Builder userApiKeyId(Long userApiKeyId) { context.userApiKeyId = userApiKeyId; return this; }
        public Builder teamId(Long teamId) { context.teamId = teamId; return this; }
        public Builder model(String model) { context.model = model; return this; }
        public Builder protocol(String protocol) { context.protocol = protocol; return this; }
        public Builder providerApiKey(String providerApiKey) { context.providerApiKey = providerApiKey; return this; }
        public Builder providerApiKeyId(Long providerApiKeyId) { context.providerApiKeyId = providerApiKeyId; return this; }
        public Builder endpoint(String endpoint) { context.endpoint = endpoint; return this; }
        public RoutingContext build() { return context; }
    }

    /**
     * 是否使用新架构路由
     */
    public boolean isNewArchitecture() {
        return productId != null;
    }

    /**
     * 获取超时时间（秒）
     */
    public int getTimeoutSeconds() {
        return 30;
    }

    // ===== Getters =====

    public Long getProviderId() { return providerId; }
    public String getProviderName() { return providerName; }
    public ProviderType getProviderType() { return providerType; }
    public Long getProductId() { return productId; }
    public ProductType getProductType() { return productType; }
    public Long getUserApiKeyId() { return userApiKeyId; }
    public Long getTeamId() { return teamId; }
    public String getModel() { return model; }
    public String getProtocol() { return protocol; }
    public String getProviderApiKey() { return providerApiKey; }
    public Long getProviderApiKeyId() { return providerApiKeyId; }
    public String getEndpoint() { return endpoint; }
}