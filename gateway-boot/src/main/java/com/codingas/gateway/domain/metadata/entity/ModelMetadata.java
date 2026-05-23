package com.codingas.gateway.domain.metadata.entity;

import com.codingas.gateway.domain.metadata.enums.MetadataState;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 模型元数据实体
 * <p>
 * 纯属性实体，不持有定价和产品关联信息。
 * 定价由产品（ProductMetadata）负责，关联由 ProductModelMetadata 承载。
 * </p>
 */
public class ModelMetadata {

    private Long id;
    private String providerId;
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private String knowledgeCutoff;
    private LocalDate releaseDate;
    private Boolean openWeights;
    private List<String> modalities;
    private Map<String, Boolean> capabilities;
    private MetadataSource source;
    private Instant sourceSyncedAt;
    private MetadataState state;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public ModelMetadata() {}

    public ModelMetadata(String providerId, String providerModelId, String displayName,
                         MetadataSource source) {
        this.providerId = providerId;
        this.providerModelId = providerModelId;
        this.displayName = displayName;
        this.source = source;
        this.state = MetadataState.ACTIVE;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderModelId() { return providerModelId; }
    public void setProviderModelId(String providerModelId) { this.providerModelId = providerModelId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getModelFamily() { return modelFamily; }
    public void setModelFamily(String modelFamily) { this.modelFamily = modelFamily; }

    public Integer getContextWindow() { return contextWindow; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }

    public Integer getMaxInputTokens() { return maxInputTokens; }
    public void setMaxInputTokens(Integer maxInputTokens) { this.maxInputTokens = maxInputTokens; }

    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public String getKnowledgeCutoff() { return knowledgeCutoff; }
    public void setKnowledgeCutoff(String knowledgeCutoff) { this.knowledgeCutoff = knowledgeCutoff; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public Boolean getOpenWeights() { return openWeights; }
    public void setOpenWeights(Boolean openWeights) { this.openWeights = openWeights; }

    public List<String> getModalities() { return modalities; }
    public void setModalities(List<String> modalities) { this.modalities = modalities; }

    public Map<String, Boolean> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Boolean> capabilities) { this.capabilities = capabilities; }

    public MetadataSource getSource() { return source; }
    public void setSource(MetadataSource source) { this.source = source; }

    public Instant getSourceSyncedAt() { return sourceSyncedAt; }
    public void setSourceSyncedAt(Instant sourceSyncedAt) { this.sourceSyncedAt = sourceSyncedAt; }

    public MetadataState getState() { return state; }
    public void setState(MetadataState state) { this.state = state; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}