package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 产品元数据 DO
 */
@Entity
@Table(name = "product_metadata")
public class ProductMetadataDo extends BaseDo {

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "product_type", nullable = false, length = 32)
    private String productType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "endpoints", nullable = false, columnDefinition = "json")
    private String endpoints;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "input_price", precision = 12, scale = 6)
    private BigDecimal inputPrice;

    @Column(name = "output_price", precision = 12, scale = 6)
    private BigDecimal outputPrice;

    @Column(name = "reasoning_price", precision = 12, scale = 6)
    private BigDecimal reasoningPrice;

    @Column(name = "cache_read_price", precision = 12, scale = 6)
    private BigDecimal cacheReadPrice;

    @Column(name = "cache_write_price", precision = 12, scale = 6)
    private BigDecimal cacheWritePrice;

    @Column(name = "input_audio_price", precision = 12, scale = 6)
    private BigDecimal inputAudioPrice;

    @Column(name = "output_audio_price", precision = 12, scale = 6)
    private BigDecimal outputAudioPrice;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    // ==================== Getter / Setter ====================

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEndpoints() { return endpoints; }
    public void setEndpoints(String endpoints) { this.endpoints = endpoints; }

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

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
