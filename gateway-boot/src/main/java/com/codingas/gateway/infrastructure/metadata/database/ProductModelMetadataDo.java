package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 产品-模型元数据关联 DO
 */
@Entity
@Table(name = "product_model_metadata", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pmm_product_model", columnNames = {"product_id", "model_id"})
})
public class ProductModelMetadataDo extends BaseDo {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_synced_at")
    private Instant sourceSyncedAt;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    // ==================== Getter / Setter ====================

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getSourceSyncedAt() { return sourceSyncedAt; }
    public void setSourceSyncedAt(Instant sourceSyncedAt) { this.sourceSyncedAt = sourceSyncedAt; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}