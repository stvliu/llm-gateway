package com.codingas.gateway.domain.metadata.entity;

import com.codingas.gateway.domain.metadata.enums.MetadataState;

/**
 * 供应商元数据实体
 * <p>
 * 包含供应商身份和连接配置，
 * 模型元数据独立存储在 ModelMetadata 中。
 * </p>
 */
public class ProviderMetadata {

    private Long id;
    private String providerId;
    private String providerName;
    private String providerType;
    private Object providerConfig;
    private String iconUrl;
    private String description;
    private Object tags;
    private MetadataState state;
    private java.time.Instant deletedAt;
    private java.time.Instant createdAt;
    private Long createdBy;
    private java.time.Instant updatedAt;
    private Long updatedBy;

    public ProviderMetadata() {}

    public ProviderMetadata(String providerId, String providerName, String providerType,
                            Object providerConfig) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerType = providerType;
        this.providerConfig = providerConfig;
        this.state = MetadataState.ACTIVE;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }

    public Object getProviderConfig() { return providerConfig; }
    public void setProviderConfig(Object providerConfig) { this.providerConfig = providerConfig; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Object getTags() { return tags; }
    public void setTags(Object tags) { this.tags = tags; }

    public MetadataState getState() { return state; }
    public void setState(MetadataState state) { this.state = state; }

    public java.time.Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(java.time.Instant deletedAt) { this.deletedAt = deletedAt; }

    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
