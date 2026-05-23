package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户 API Key 数据对象
 */
@Entity
@Table(name = "user_api_keys")
public class UserApiKeyDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(
            name = "user_api_key_products",
            joinColumns = @JoinColumn(name = "user_api_key_id")
    )
    @Column(name = "product_id")
    private Set<Long> productIds = new HashSet<>();

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(name = "key_encrypted", columnDefinition = "TEXT")
    private String keyEncrypted;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "models", length = 2000)
    private String models;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserApiKeyState state;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Set<Long> getProductIds() { return productIds; }
    public void setProductIds(Set<Long> productIds) { this.productIds = productIds; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getKeyEncrypted() { return keyEncrypted; }
    public void setKeyEncrypted(String keyEncrypted) { this.keyEncrypted = keyEncrypted; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModels() { return models; }
    public void setModels(String models) { this.models = models; }

    public Long getQuotaLimit() { return quotaLimit; }
    public void setQuotaLimit(Long quotaLimit) { this.quotaLimit = quotaLimit; }

    public UserApiKeyState getState() { return state; }
    public void setState(UserApiKeyState state) { this.state = state; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
