package com.codingas.gateway.infrastructure.audit.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 使用记录 DO
 *
 * <p>JPA 实体，对应数据库 usage_logs 表。</p>
 * <p>已移除对 GatewayApiKeyDo 的 JPA 关联，使用 userApiKeyId 字段替代。</p>
 */
@Entity
@Table(name = "usage_logs", indexes = {
    @Index(name = "idx_usage_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_usage_provider_created", columnList = "provider_id, created_at"),
    @Index(name = "idx_usage_key_created", columnList = "user_api_key_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsageLogDo extends BaseDo {

    /**
     * 用户 API Key ID（新架构）
     * <p>替代旧的 gateway_api_key_id 外键关联</p>
     */
    @Column(name = "user_api_key_id", nullable = false)
    private Long userApiKeyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDo user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderDo provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelDo model;

    /** 新架构：团队 ID */
    @Column(name = "team_id")
    private Long teamId;

    /** 新架构：产品 ID */
    @Column(name = "product_id")
    private Long productId;

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
