package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志实体
 *
 * <p>记录所有 API 请求、安全事件和系统操作审计日志。</p>
 *
 * <p>表名: audit_logs</p>
 *
 * @see BaseEntity
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_logs_trace_id", columnList = "trace_id")
    }
)
@Getter
@Setter
public class AuditLog extends BaseEntity {

    /**
     * 用户 ID (FK → users.id, 执行操作的用户)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 操作类型
     *
     * <p>如 "API_CALL", "AUTH_SUCCESS", "AUTH_FAILURE", "CONFIG_CHANGE"</p>
     */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    /**
     * 访问的资源
     *
     * <p>如 "/v1/chat/completions", "/v1/messages"</p>
     */
    @Column(name = "resource", length = 128)
    private String resource;

    /**
     * HTTP 请求方法
     */
    @Column(name = "request_method", length = 16)
    private String requestMethod;

    /**
     * 请求 URL
     */
    @Column(name = "request_path", length = 256)
    private String requestPath;

    /**
     * 请求体 (已脱敏)
     *
     * <p>敏感数据如 API Key、密码等已用 **** 替换</p>
     */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /**
     * HTTP 响应状态码
     */
    @Column(name = "response_status")
    private Integer responseStatus;

    /**
     * 响应时间 (毫秒)
     */
    @Column(name = "response_time")
    private Integer responseTime;

    /**
     * OpenTelemetry _trace ID
     */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /**
     * 客户端 IP 地址
     */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * 浏览器/客户端信息
     */
    @Column(name = "user_agent", length = 256)
    private String userAgent;

    /**
     * 错误消息 (请求失败时)
     */
    @Column(name = "error_message", length = 512)
    private String errorMessage;
}
