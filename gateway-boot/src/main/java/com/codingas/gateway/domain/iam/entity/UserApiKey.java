package com.codingas.gateway.domain.iam.entity;

import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 领域实体
 * <p>
 * 一个 Key 归属一个用户，可关联多个产品，路由时按 model name 匹配对应的 Product。
 * keyHash 用于认证验证，keyPlain 用于创建时传入和详情展示（由基础设施层加解密）。
 * </p>
 */
@Setter
@Getter
public class UserApiKey {

    private Long id;
    private Long userId;
    private List<Long> channelIds;
    /**
     * -- GETTER --
     * Key 哈希（SHA-256），用于认证验证，由基础设施层在 save 时计算
     */
    private String keyHash;
    private String keyPrefix;
    /**
     * -- GETTER --
     * Key 明文，创建时设置，查询时由基础设施层解密填充
     */
    private String keyPlain;
    private String name;
    private List<String> models;
    private Long quotaLimit;
    private UserApiKeyState state;
    private Instant createdAt;
    private Instant updatedAt;

    public UserApiKey() {
    }

    /** 检查密钥是否可用（简单状态判断，允许保留在实体中） */
    public boolean isAvailable() {
        return state != null && state.isAvailable();
    }

    /**
     * 重写 toString 排除敏感字段
     */
    @Override
    public String toString() {
        return "UserApiKey{" +
                "id=" + id +
                ", userId=" + userId +
                ", channelIds=" + channelIds +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                '}';
    }
}