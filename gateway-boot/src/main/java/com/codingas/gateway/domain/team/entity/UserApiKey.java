package com.codingas.gateway.domain.team.entity;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 领域实体
 * <p>
 * 一个 Key 可关联多个产品，路由时按 model name 匹配对应的 Product。
 * keyHash 用于认证验证，keyPlain 用于创建时传入和详情展示（由基础设施层加解密）。
 * </p>
 */
public class UserApiKey {

    private Long id;
    private Long teamId;
    private Long userId;
    private List<Long> productIds;
    private String keyHash;
    private String keyPrefix;
    private String keyPlain;
    private String name;
    private List<String> models;
    private Long quotaLimit;
    private UserApiKeyState state;
    private Instant createdAt;
    private Instant updatedAt;

    public UserApiKey() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }

    /** Key 哈希（SHA-256），用于认证验证，由基础设施层在 save 时计算 */
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    /** Key 明文，创建时设置，查询时由基础设施层解密填充 */
    public String getKeyPlain() { return keyPlain; }
    public void setKeyPlain(String keyPlain) { this.keyPlain = keyPlain; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public Long getQuotaLimit() { return quotaLimit; }
    public void setQuotaLimit(Long quotaLimit) { this.quotaLimit = quotaLimit; }

    public UserApiKeyState getState() { return state; }
    public void setState(UserApiKeyState state) { this.state = state; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

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
                ", teamId=" + teamId +
                ", userId=" + userId +
                ", productIds=" + productIds +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                '}';
    }
}