package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

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

    private User user;

    private String name;

    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    private Instant expiresAt;

    private Instant lastUsedAt;

    private List<String> ipWhitelist;

    private Instant deletedAt;

    public enum ApiKeyStatus {
        /** 正常 */
        ACTIVE,
        /** 禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }

    /**
     * 检查凭证是否有效
     */
    public boolean isValid() {
        if (ApiKeyStatus.ACTIVE.equals(status)) {
            if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                return false;
            }
            return true;
        }
        return false;
    }
}
