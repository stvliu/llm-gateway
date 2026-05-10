package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

/**
 * 网关访问凭证实体
 *
 * <p>用户调用 LLM-Gateway 网关的凭据，格式为 sk-xxxxxxxx。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class GatewayApiKey extends BaseEntity {

    private String keyHash;

    private Long userId;

    private String username;

    private String name;

    private GatewayApiKeyState state = GatewayApiKeyState.ACTIVE;

    private Instant expiresAt;

    private Instant lastUsedAt;

    private List<String> ipWhitelist;

    private Instant deletedAt;

    /**
     * 检查凭证是否有效
     */
    public boolean isValid() {
        if (GatewayApiKeyState.ACTIVE.equals(state)) {
            if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                return false;
            }
            return true;
        }
        return false;
    }
}
