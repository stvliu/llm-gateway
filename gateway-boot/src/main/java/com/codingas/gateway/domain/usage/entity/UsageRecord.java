package com.codingas.gateway.domain.usage.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 使用记录实体
 *
 * <p>记录每次 API 调用的详细信息，用于用量分析和成本统计。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class UsageRecord extends BaseEntity {

    private String logCode;

    private Long gatewayApiKeyId;

    private Long userId;

    private Long providerId;

    private Long modelId;

    /** 新架构：用户 API Key ID */
    private Long userApiKeyId;

    /** 新架构：团队 ID */
    private Long teamId;

    /** 新架构：产品 ID */
    private Long productId;

    private String requestId;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private Integer latencyMs;

    private String statusCode;

    private String errorMessage;

    private Boolean failover = false;

    private ApiFormat apiFormat;

    public enum ApiFormat {
        OPENAI,
        ANTHROPIC
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return statusCode != null && statusCode.startsWith("2");
    }
}
