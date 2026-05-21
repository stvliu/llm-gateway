package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 供应商元数据 DO
 */
@Entity
@Table(name = "provider_metadata")
public class ProviderMetadataDo extends BaseDo {

    @Column(name = "provider_id", nullable = false, unique = true, length = 64)
    private String providerId;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Column(name = "provider_config", nullable = false, columnDefinition = "json")
    private String providerConfig;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ==================== Getter / Setter ====================

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderConfig() { return providerConfig; }
    public void setProviderConfig(String providerConfig) { this.providerConfig = providerConfig; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
