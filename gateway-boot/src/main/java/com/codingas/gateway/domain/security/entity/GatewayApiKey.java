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
 * <p>旧架构实体，由 UserApiKey + ProductApiKey 替代。</p>
 *
 * @deprecated 使用 {@link com.codingas.gateway.domain.team.entity.UserApiKey} 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class GatewayApiKey extends BaseEntity {

    private String keyHash;

    private String keyEncrypted;

    private Long userId;

    private String username;

    private String name;

    private GatewayApiKeyState state = GatewayApiKeyState.ACTIVE;

    private Instant expiresAt;

    private Instant lastUsedAt;

    private List<String> ipWhitelist;

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
