package com.codingas.gateway.infrastructure.audit.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import com.codingas.gateway.infrastructure.apikey.gateway.database.dataobject.GatewayApiKeyDo;
import com.codingas.gateway.infrastructure.user.gateway.database.dataobject.UserDo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 使用记录 DO
 *
 * <p>JPA 实体，对应数据库 usage_logs 表。</p>
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
public class UsageLogDo extends BaseDo {

    @Column(name = "log_code", nullable = false, unique = true, length = 64)
    private String logCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_api_key_id", nullable = false)
    private GatewayApiKeyDo gatewayApiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDo user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderDo provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelDo model;

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
