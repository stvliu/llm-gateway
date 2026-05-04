package com.codingas.gateway.domain.proxy.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 语义缓存实体
 *
 * <p>用于缓存相似请求的响应，减少重复调用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class SemanticCache extends BaseEntity {

    private String cacheCode;

    private String queryHash;

    private String queryEmbedding;

    private Double similarityThreshold;

    private String responseContent;

    private Long modelId;

    private Instant expiresAt;

    private Integer hitCount;

    /**
     * 检查缓存是否有效
     */
    public boolean isValid() {
        return expiresAt == null || Instant.now().isBefore(expiresAt);
    }
}
