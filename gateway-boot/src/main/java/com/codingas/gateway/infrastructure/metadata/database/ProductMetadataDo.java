package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

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

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
