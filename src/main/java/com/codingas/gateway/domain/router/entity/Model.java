package com.codingas.gateway.domain.router.entity;
import com.codingas.gateway.domain.DomainEntity;

import com.codingas.gateway.domain.BaseEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 模型实体
 *
 * <p>表示具体的 AI 模型，是调用的最小单位。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Model extends BaseEntity {

    private String modelCode;

    private Provider provider;

    private String providerModelId;

    private String displayName;

    private Integer contextWindow;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Map<String, Boolean> capabilities;

    private ModelStatus status = ModelStatus.ACTIVE;

    private Instant deletedAt;

    public enum ModelStatus {
        /** 正常 */
        ACTIVE,
        /** 已废弃 */
        DEPRECATED,
        /** 已删除 */
        DELETED
    }

    /**
     * 检查模型是否可用
     */
    public boolean isAvailable() {
        return ModelStatus.ACTIVE.equals(status);
    }
}
