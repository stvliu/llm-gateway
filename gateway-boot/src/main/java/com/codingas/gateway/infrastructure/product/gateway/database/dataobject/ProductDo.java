package com.codingas.gateway.infrastructure.product.gateway.database.dataobject;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产品数据对象
 */
@Data
@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_products_provider_name", columnNames = {"provider_id", "name"})
})
public class ProductDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "product_type", nullable = false, length = 32)
    private String productType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "models", columnDefinition = "text")
    private List<String> models;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoints", columnDefinition = "text")
    private Map<String, String> endpoints;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "state", length = 16)
    private String state;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}