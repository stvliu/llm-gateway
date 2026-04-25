package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 网关访问凭证
 *
 * <p>用户调用 LLM-Gateway 网关的凭据，属于用户维度。</p>
 * <p>一个用户可持有多个 Provider 的访问权限，同一 Provider 下可有多个 Key（主备/轮换）。</p>
 */
@Entity
@Table(name = "gateway_api_keys", indexes = {
    @Index(name = "idx_key_hash", columnList = "key_hash"),
    @Index(name = "idx_user_provider", columnList = "user_id, provider_id")
})
@Getter
@Setter
public class GatewayApiKey extends BaseEntity {

    /**
     * 密钥编码 (业务标识)
     */
    @Column(name = "key_code", nullable = false, unique = true, length = 128)
    private String keyCode;

    /**
     * Key 哈希 (用于验证)
     */
    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;

    /**
     * 用户 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 关联的 Provider ID（NULL 表示全部 Provider）
     */
    @Column(name = "provider_id")
    private Long providerId;

    /**
     * 路由分组 ID（NULL 表示使用默认路由策略）
     */
    @Column(name = "route_group_id")
    private Long routeGroupId;

    /**
     * 密钥名称（如"主Key"、"备用Key"）
     */
    @Column(name = "name", length = 64)
    private String name;

    /**
     * 密钥状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GatewayApiKeyStatus status = GatewayApiKeyStatus.ACTIVE;

    /**
     * 过期时间（NULL 表示永不过期）
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 模型白名单 (JSON 数组，NULL 表示全部允许)
     */
    @Column(name = "model_whitelist", columnDefinition = "JSON")
    private String modelWhitelist;

    /**
     * IP 白名单 (JSON 数组，支持 CIDR，NULL 表示全部允许)
     */
    @Column(name = "ip_whitelist", columnDefinition = "JSON")
    private String ipWhitelist;

    /**
     * 网关访问凭证状态枚举
     */
    public enum GatewayApiKeyStatus {
        /** 活跃 */
        ACTIVE,
        /** 已禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }
}
