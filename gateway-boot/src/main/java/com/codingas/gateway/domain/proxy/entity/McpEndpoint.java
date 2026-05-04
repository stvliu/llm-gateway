package com.codingas.gateway.domain.proxy.entity;

import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * MCP 端点实体
 *
 * <p>Model Context Protocol 端点配置。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class McpEndpoint extends BaseEntity {

    private String endpointCode;

    private String name;

    private String endpointUrl;

    private String protocol;

    private String authType;

    private String authCredentials;

    private McpEndpointStatus status = McpEndpointStatus.ACTIVE;

    private Instant lastConnectedAt;

    public enum McpEndpointStatus {
        /** 活跃 */
        ACTIVE,
        /** 已断开 */
        DISCONNECTED,
        /** 已禁用 */
        DISABLED
    }

    /**
     * 检查端点是否可用
     */
    public boolean isAvailable() {
        return McpEndpointStatus.ACTIVE.equals(status);
    }
}
