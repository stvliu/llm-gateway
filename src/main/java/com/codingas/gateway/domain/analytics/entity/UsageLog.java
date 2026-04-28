package com.codingas.gateway.domain.analytics.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 使用记录实体
 *
 * <p>记录每次 API 调用的详细信息，用于用量分析和成本统计。</p>
 */
@Entity
@Table(name = "usage_logs", indexes = {
    @Index(name = "idx_usage_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_usage_provider_created", columnList = "provider_id, created_at"),
    @Index(name = "idx_usage_key_created", columnList = "gateway_api_key_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsageLog extends BaseEntity {

    @Column(name = "log_code", nullable = false, unique = true, length = 64)
    private String logCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_api_key_id", nullable = false)
    private GatewayApiKey gatewayApiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "status_code", length = 32)
    private String statusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failover")
    private Boolean failover = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_format", nullable = false)
    private ApiFormat apiFormat;

    public enum ApiFormat {
        OPENAI,
        ANTHROPIC
    }
}