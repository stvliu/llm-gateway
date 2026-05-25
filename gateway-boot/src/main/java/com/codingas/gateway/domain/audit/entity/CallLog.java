package com.codingas.gateway.domain.audit.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 调用日志实体
 *
 * <p>记录每次模型调用的全链路信息：渠道、端点、凭证、协议、耗时、成功/失败。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class CallLog extends BaseEntity {

    private String traceId;

    private Long userId;

    private String model;

    private Long channelId;

    private Long channelEndpointId;

    private String inboundProtocol;

    private String upstreamProtocol;

    private Long durationMs;

    private Boolean success;

    private Integer inputTokens;

    private Integer outputTokens;

    private String errorMessage;

    private Instant calledAt;
}
