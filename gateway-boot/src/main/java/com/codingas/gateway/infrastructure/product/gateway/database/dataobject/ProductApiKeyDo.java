package com.codingas.gateway.infrastructure.product.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品 API Key 数据对象
 */
@Data
@Entity
@Table(name = "product_api_keys")
public class ProductApiKeyDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "api_key_hash", nullable = false, length = 128)
    private String apiKeyHash;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT", nullable = false)
    private String apiKeyEncrypted;

    @Column(name = "api_key_prefix", length = 16)
    private String apiKeyPrefix;


    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "weight")
    private Integer weight = 1;

    @Column(name = "priority")
    private Integer priority = 1;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}