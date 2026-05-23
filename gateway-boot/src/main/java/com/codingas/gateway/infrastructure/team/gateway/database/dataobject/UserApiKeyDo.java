package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 用户 API Key 数据对象
 */
@Entity
@Table(name = "user_api_keys")
public class UserApiKeyDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "key_prefix", nullable = false, length = 16, unique = true)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(name = "key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String keyEncrypted;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "models", length = 1000)
    private String models;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private UserApiKeyState state;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserApiKeyDo() {
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getKeyEncrypted() { return keyEncrypted; }
    public void setKeyEncrypted(String keyEncrypted) { this.keyEncrypted = keyEncrypted; }
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