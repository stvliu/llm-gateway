package com.codingas.gateway.domain.metadata.entity;

import com.codingas.gateway.domain.metadata.enums.MetadataState;

import java.time.Instant;

/**
 * 产品-模型元数据关联实体
 * <p>
 * 纯关联实体，仅承载产品与模型的多对多关系，不含定价信息。
 * </p>
 */
public class ProductModelMetadata {

    private Long id;
    private Long productId;
    private Long modelId;
    private MetadataSource source;
    private Instant sourceSyncedAt;
    private MetadataState state;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public ProductModelMetadata() {}

    public ProductModelMetadata(Long productId, Long modelId, MetadataSource source) {
        this.productId = productId;
        this.modelId = modelId;
        this.source = source;
        this.state = MetadataState.ACTIVE;
    }

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }

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
