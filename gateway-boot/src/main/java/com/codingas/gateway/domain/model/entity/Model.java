package com.codingas.gateway.domain.model.entity;
import com.codingas.gateway.common.entity.DomainEntity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.model.enums.ModelState;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 模型实体
 *
 * <p>表示具体的 AI 模型，是调用的最小单位。</p>
 * <p>关联 Provider 通过 providerId 引用，不持有 Provider 对象。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Model extends BaseEntity {

    private Long providerId;

    private String providerName;

    private String providerModelId;

    private String displayName;

    private Integer contextWindow;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Map<String, Boolean> capabilities;

    private ModelState state = ModelState.ACTIVE;

    /**
     * 检查模型是否可用
     */
    public boolean isAvailable() {
        return ModelState.ACTIVE.equals(state);
    }
}
