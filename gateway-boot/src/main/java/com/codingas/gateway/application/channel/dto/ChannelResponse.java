package com.codingas.gateway.application.channel.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 渠道响应
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
}
