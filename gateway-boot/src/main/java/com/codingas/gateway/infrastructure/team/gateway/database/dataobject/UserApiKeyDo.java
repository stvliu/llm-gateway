package com.codingas.gateway.infrastructure.team.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 API Key 数据对象
 */
@Data
@Entity
@Table(name = "user_api_keys")
public class UserApiKeyDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "name", length = 128)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models", columnDefinition = "jsonb")
    private List<String> models;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
