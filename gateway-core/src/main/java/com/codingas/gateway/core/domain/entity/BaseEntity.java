package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 实体基类
 *
 * <p>所有业务实体必须继承此类，包含审计字段。</p>
 *
 * @see <a href="https://docs.llm-gateway.dev/data-model#audit-fields">审计字段要求</a>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 主键 (自增 BIGINT)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建人 ID (FK → users.id, 系统生成填 0L)
     */
    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 最后更新人 ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 最后更新时间
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * 删除人 ID (软删除)
     */
    @Column(name = "deleted_by")
    private Long deletedBy;

    /**
     * 删除时间 (软删除)
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    @Column(name = "version")
    private Integer version = 0;

    /**
     * 实体是否已删除
     */
    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
