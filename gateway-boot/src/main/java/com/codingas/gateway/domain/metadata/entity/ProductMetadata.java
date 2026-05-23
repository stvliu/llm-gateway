package com.codingas.gateway.domain.metadata.entity;

import com.codingas.gateway.domain.metadata.enums.MetadataState;
import com.codingas.gateway.domain.metadata.enums.ProductType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据实体
 * <p>
 * 表示供应商提供的产品/套餐，包含多协议端点配置。
 * 一个供应商可以有多个产品（如按量计费、Coding Plan）。
 * </p>
 */
public class ProductMetadata {

    private Long id;
    private String providerId;
    private String productName;
    private ProductType productType;
    private String description;
    private Map<String, String> endpoints;  // protocol -> base_url
    private Boolean isDefault;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal reasoningPrice;
    private BigDecimal cacheReadPrice;
    private BigDecimal cacheWritePrice;
    private BigDecimal inputAudioPrice;
    private BigDecimal outputAudioPrice;
    private MetadataState state;
    private MetadataSource source;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public ProductMetadata() {}

    public ProductMetadata(String providerId, String productName, ProductType productType) {
        this.providerId = providerId;
        this.productName = productName;
        this.productType = productType;
        this.isDefault = false;
        this.state = MetadataState.ACTIVE;
        this.source = MetadataSource.BUILTIN;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getEndpoints() { return endpoints; }
    public void setEndpoints(Map<String, String> endpoints) { this.endpoints = endpoints; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public BigDecimal getInputPrice() { return inputPrice; }
    public void setInputPrice(BigDecimal inputPrice) { this.inputPrice = inputPrice; }

    public BigDecimal getOutputPrice() { return outputPrice; }
    public void setOutputPrice(BigDecimal outputPrice) { this.outputPrice = outputPrice; }

    public BigDecimal getReasoningPrice() { return reasoningPrice; }
    public void setReasoningPrice(BigDecimal reasoningPrice) { this.reasoningPrice = reasoningPrice; }

    public BigDecimal getCacheReadPrice() { return cacheReadPrice; }
    public void setCacheReadPrice(BigDecimal cacheReadPrice) { this.cacheReadPrice = cacheReadPrice; }

    public BigDecimal getCacheWritePrice() { return cacheWritePrice; }
    public void setCacheWritePrice(BigDecimal cacheWritePrice) { this.cacheWritePrice = cacheWritePrice; }

    public BigDecimal getInputAudioPrice() { return inputAudioPrice; }
    public void setInputAudioPrice(BigDecimal inputAudioPrice) { this.inputAudioPrice = inputAudioPrice; }

    public BigDecimal getOutputAudioPrice() { return outputAudioPrice; }
    public void setOutputAudioPrice(BigDecimal outputAudioPrice) { this.outputAudioPrice = outputAudioPrice; }

    public MetadataState getState() { return state; }
    public void setState(MetadataState state) { this.state = state; }

    public MetadataSource getSource() { return source; }
    public void setSource(MetadataSource source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
