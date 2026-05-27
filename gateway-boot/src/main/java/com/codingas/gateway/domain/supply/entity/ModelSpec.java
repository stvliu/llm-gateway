package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 模型规格实体（从 Model 拆出规格部分）
 *
 * <p>ModelSpec 是模型固有规格，与渠道无关。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class ModelSpec extends BaseEntity {

    
    /** 供应商侧标识（如 "gpt-4o"，路由匹配用） */
    private String providerModelId;

    private String displayName;

    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private ModelSpecState state = ModelSpecState.ACTIVE;

    /** 路由优先级（数值越小优先级越高） */
    private Integer priority;

    /** 负载均衡权重 */
    private Integer weight;

    /**
     * 检查模型规格是否可用
     */
    public boolean isAvailable() {
        return ModelSpecState.ACTIVE.equals(state);
    }
}