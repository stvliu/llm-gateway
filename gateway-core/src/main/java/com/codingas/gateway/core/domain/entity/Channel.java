package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 渠道实体
 *
 * <p>通往某个 Provider 的具体连接实例，包含 API Key、BaseURL 等配置。</p>
 */
@Entity
@Table(name = "channels")
@Getter
@Setter
public class Channel extends BaseEntity {

    /**
     * 渠道编码 (业务标识)
     */
    @Column(name = "channel_code", nullable = false, unique = true, length = 64)
    private String channelCode;

    /**
     * 渠道名称
     */
    @Column(name = "channel_name", nullable = false, length = 128)
    private String channelName;

    /**
     * 所属团队 ID
     */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /**
     * 所属供应商 ID
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /**
     * 所属分组 ID
     */
    @Column(name = "group_id")
    private Long groupId;

    /**
     * 支持的模型列表 (JSON)
     */
    @Column(name = "models", columnDefinition = "JSON")
    private String models;

    /**
     * 渠道状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChannelStatus status = ChannelStatus.ACTIVE;

    /**
     * 优先级 (数值越小优先级越高)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    /**
     * 超时时间 (秒)
     */
    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 30;

    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 3;

    /**
     * RPM 限制 (每分钟请求数)
     */
    @Column(name = "rpm_limit")
    private Integer rpmLimit;

    /**
     * TPM 限制 (每分钟 Token 数)
     */
    @Column(name = "tpm_limit")
    private Integer tpmLimit;

    /**
     * API 地址 (支持代理/镜像)
     */
    @Column(name = "base_url", length = 512)
    private String baseUrl;

    /**
     * 渠道状态枚举
     */
    public enum ChannelStatus {
        /** 活跃 */
        ACTIVE,
        /** 禁用 */
        DISABLED,
        /** 错误 */
        ERROR
    }
}
