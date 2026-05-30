package com.codingas.gateway.domain.audit.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 调用日志实体
 *
 * <p>记录每次模型调用的全链路信息。</p>
 */
@Getter
@Setter
public class CallLog {

    private Long id;
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