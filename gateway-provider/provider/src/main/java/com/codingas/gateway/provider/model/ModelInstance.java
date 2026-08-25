/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.model;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}