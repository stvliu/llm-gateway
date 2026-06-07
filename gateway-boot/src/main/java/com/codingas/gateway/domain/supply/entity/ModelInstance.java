package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ModelInstance extends BaseEntity {

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
    private ChannelModelState state = ChannelModelState.ACTIVE;

    /**
     * 检查是否可用
     */
    public boolean isAvailable() {
        return ChannelModelState.ACTIVE.equals(state);
    }
}