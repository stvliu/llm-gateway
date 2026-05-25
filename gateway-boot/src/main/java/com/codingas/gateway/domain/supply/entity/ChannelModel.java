package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 渠道模型关联实体（替代 ProductModel）
 *
 * <p>从纯关联实体升级为带定价的关联实体，定价随模型独立变更。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class ChannelModel extends BaseEntity {

    private Long channelId;

    private Long modelSpecId;

    /** 输入价格（每百万 Token） */
    private BigDecimal inputPrice;

    /** 输出价格（每百万 Token） */
    private BigDecimal outputPrice;

    /** 推理价格（每百万 Token） */
    private BigDecimal reasoningPrice;

    /** 缓存读取价格（每百万 Token） */
    private BigDecimal cacheReadPrice;

    /** 缓存写入价格（每百万 Token） */
    private BigDecimal cacheWritePrice;

    /** 输入音频价格（每百万 Token） */
    private BigDecimal inputAudioPrice;

    /** 输出音频价格（每百万 Token） */
    private BigDecimal outputAudioPrice;

    /** 订阅模式下的 Token 额度限制 */
    private Long quotaLimit;

    private ChannelModelState state = ChannelModelState.ACTIVE;

    /**
     * 检查是否可用
     */
    public boolean isAvailable() {
        return ChannelModelState.ACTIVE.equals(state);
    }
}