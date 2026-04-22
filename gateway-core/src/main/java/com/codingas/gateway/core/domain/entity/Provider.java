package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 模型供应商实体
 */
@Entity
@Table(name = "model_providers")
@Getter
@Setter
public class Provider extends BaseEntity {

    /**
     * 供应商编码 (业务标识)
     */
    @Column(name = "provider_code", nullable = false, unique = true, length = 64)
    private String providerCode;

    /**
     * 供应商名称
     */
    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    /**
     * 供应商官网 URL
     */
    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    /**
     * API 文档链接
     */
    @Column(name = "api_doc_url", length = 512)
    private String apiDocUrl;

    /**
     * 供应商状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProviderStatus status = ProviderStatus.ACTIVE;

    /**
     * 供应商状态枚举
     */
    public enum ProviderStatus {
        /** 活跃 */
        ACTIVE,
        /** 停用 */
        DISABLED
    }
}
