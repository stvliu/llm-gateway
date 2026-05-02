package com.codingas.gateway.domain.audit.entity;

import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 调用日志实体
 *
 * <p>记录每次 API 调用的详细技术信息。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class CallLog extends BaseEntity {

    private String logCode;

    private String traceId;

    private Long gatewayApiKeyId;

    private Long userId;

    private String requestMethod;

    private String requestPath;

    private String requestModel;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer latencyMs;

    private Integer responseStatus;

    private String providerCode;

    private String modelName;

    private String errorMessage;

    private Instant calledAt;
}
