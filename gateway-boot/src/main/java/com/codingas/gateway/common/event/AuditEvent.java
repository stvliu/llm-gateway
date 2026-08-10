/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.common.event;

import java.time.Instant;

/**
 * 审计事件
 *
 * <p>记录所有安全相关的操作事件。</p>
 *
 * @param userId 用户 ID
 * @param apiKeyId API Key ID
 * @param teamId 团队 ID
 * @param action 操作类型 (API_CALL, AUTH_SUCCESS, AUTH_FAILURE, CONFIG_CHANGE)
 * @param resource 访问的资源路径
 * @param clientIp 客户端 IP
 * @param userAgent 用户代理
 * @param responseStatus HTTP 响应状态码
 * @param traceId OpenTelemetry Trace ID
 * @param occurredOn 发生时间
 */
public record AuditEvent(
        Long userId,
        Long apiKeyId,
        Long teamId,
        String action,
        String resource,
        String clientIp,
        String userAgent,
        Integer responseStatus,
        String traceId,
        Instant occurredOn
) implements DomainEvent {

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    /**
     * 操作类型枚举
     */
    public enum Action {
        API_CALL,
        AUTH_SUCCESS,
        AUTH_FAILURE,
        CONFIG_CHANGE,
        RATE_LIMIT_EXCEEDED,
        IP_BLOCKED,
        TOKEN_EXCEEDED
    }

    public static class Builder {
        private Long userId;
        private Long apiKeyId;
        private Long teamId;
        private String action;
        private String resource;
        private String clientIp;
        private String userAgent;
        private Integer responseStatus;
        private String traceId;
        private Instant occurredOn = Instant.now();

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder apiKeyId(Long apiKeyId) {
            this.apiKeyId = apiKeyId;
            return this;
        }

        public Builder teamId(Long teamId) {
            this.teamId = teamId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder responseStatus(Integer responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder occurredOn(Instant occurredOn) {
            this.occurredOn = occurredOn;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(
                    userId, apiKeyId, teamId, action, resource,
                    clientIp, userAgent, responseStatus, traceId, occurredOn
            );
        }
    }
}
