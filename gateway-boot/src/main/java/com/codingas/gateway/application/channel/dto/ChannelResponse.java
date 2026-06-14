package com.codingas.gateway.application.channel.dto;

import com.codingas.gateway.domain.supply.enums.ChannelHealthSource;
import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道响应
 *
 * <p>包含渠道基本信息、端点列表，以及最近一次连通性测试的健康摘要（向后兼容：
 * 三个 lastHealthXxx 字段在未测试过时为 null）。</p>
 */
@Data
public class ChannelResponse {

    private Long id;

    private Long providerId;

    /** 供应商名称（仅展示用，从 Provider 查找） */
    private String providerName;

    private String name;

    private String billingMode;

    private Long quotaLimit;

    private Integer timeout;

    private Integer maxRetries;

    private String state;

    private List<ChannelEndpointResponse> endpoints;

    private Instant createdAt;

    private Instant updatedAt;

    /** 最近一次连通性测试完成时间；从未测试过时为 null */
    private Instant lastHealthCheckAt;

    /** 最近一次健康聚合状态；从未测试过时为 null */
    private ChannelHealthStatus lastHealthStatus;

    /** 最近一次测试触发来源；仅 CARD/DRAWER 持久化，从未测试过时为 null */
    private ChannelHealthSource lastHealthSource;
}
