package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 渠道实体（替代 Product）
 *
 * <p>一个渠道对应一个端点和一个协议，多协议需求通过建多个 Channel 解决。</p>
 * <p>兼容原 Product 实体的 providerName, endpoints, pricing, quotaLimit 等字段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Channel extends BaseEntity {

    private Long providerId;

    /** 供应商名称（冗余，便于显示） */
    private String providerName;

    private String name;

    /** 单一端点 URL */
    private String endpointUrl;

    /** 单一协议类型 */
    private Protocol protocol;

    /** 计费模式 */
    private BillingMode billingMode;

    /** 端点映射（JSON 字符串存储，兼容原 Product.endpoints 字段） */
    private String endpoints;

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

    /** 输入音频价格 */
    private BigDecimal inputAudioPrice;

    /** 输出音频价格 */
    private BigDecimal outputAudioPrice;

    /** 配额限制（Token 数） */
    private Long quotaLimit;

    private Integer priority;

    private Integer weight;

    private Integer timeout;

    private Integer maxRetries;

    private ChannelState state = ChannelState.ACTIVE;

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 检查渠道是否可用
     */
    public boolean isAvailable() {
        return ChannelState.ACTIVE.equals(state);
    }
}