package com.codingas.gateway.domain.iam.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户 API Key 领域实体
 * <p>
 * 一个 Key 归属一个用户，通过用户所属团队继承渠道访问权限。
 * keyHash 用于认证验证，keyPlain 用于创建时传入和详情展示（由基础设施层加解密）。
 * </p>
 */
@Setter
@Getter
public class UserApiKey {

    private Long id;
    private Long userId;
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
    /** 是否已删除（逻辑删除） */
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;

    public UserApiKey() {
    }

    /** 检查密钥是否可用 */
    public boolean isAvailable() {
        return !deleted;
    }

    /**
     * 重写 toString 排除敏感字段
     */
    @Override
    public String toString() {
        return "UserApiKey{" +
                "id=" + id +
                ", userId=" + userId +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", name='" + name + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}