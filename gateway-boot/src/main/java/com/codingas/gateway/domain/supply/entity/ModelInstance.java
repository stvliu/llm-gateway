/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型实例实体（替代 ChannelModel）
 *
 * <p>表示渠道上的模型实例配置，支持覆盖模型规格的特定属性。</p>
 * <p>定价信息已移至 Model 实体统一管理，此处仅保留覆盖配置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ModelInstance extends BaseEntity {

    /**
     * 模型实例生命周期状态
     */
    public enum State {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEPRECATED,
        RETIRED;

        public boolean isRoutable() {
            return this == ACTIVE || this == DEPRECATED;
        }

        public boolean isTerminal() {
            return this == RETIRED;
        }

        public boolean canTransitionTo(State target) {
            return switch (this) {
                case PENDING    -> target == ACTIVE;
                case ACTIVE     -> target == SUSPENDED || target == DEPRECATED;
                case SUSPENDED  -> target == ACTIVE    || target == DEPRECATED || target == RETIRED;
                case DEPRECATED -> target == RETIRED;
                case RETIRED    -> false;
            };
        }
    }

    /** 所属渠道 ID */
    private Long channelId;

    /** 关联的模型规格 ID */
    private Long modelId;

    /** 上游模型名，null 表示与 Model.modelName 相同 */
    private String upstreamModelName;

    /** 能力覆盖配置（覆盖 Model.capabilities） */
    private Map<String, Boolean> capabilitiesOverride;

    /** 上下文窗口覆盖（覆盖 Model.contextWindow） */
    private Integer contextWindowOverride;

    /** 优先级（用于同模型多渠道选择，默认 100） */
    private Integer priority = 100;

    /** 权重（用于同优先级渠道的负载均衡，默认 100） */
    private Integer weight = 100;

    /** 订阅模式下的 Token 额度限制 */
    private Long quotaLimit;

    /** 实例状态 */
    private State state = State.PENDING;

    /**
     * 检查是否可用
     */
    public boolean isAvailable() {
        return State.ACTIVE.equals(state);
    }

    /**
     * 解析有效能力：capabilitiesOverride 覆盖 Model.capabilities 的默认值
     * 当 override 中包含某个能力键时使用覆盖值，否则使用模型默认值
     */
    public Map<String, Boolean> resolveCapabilities(Map<String, Boolean> modelCapabilities) {
        if (capabilitiesOverride == null || capabilitiesOverride.isEmpty()) {
            return modelCapabilities;
        }
        if (modelCapabilities == null || modelCapabilities.isEmpty()) {
            return capabilitiesOverride;
        }
        Map<String, Boolean> resolved = new LinkedHashMap<>(modelCapabilities);
        resolved.putAll(capabilitiesOverride);
        return resolved;
    }

    /**
     * 解析有效上下文窗口：contextWindowOverride 覆盖 Model.contextWindow
     * 当 override 不为 null 时使用覆盖值，否则使用模型默认值
     */
    public Integer resolveContextWindow(Integer modelContextWindow) {
        return contextWindowOverride != null ? contextWindowOverride : modelContextWindow;
    }
}