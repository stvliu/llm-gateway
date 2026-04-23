package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.domain.enums.ProviderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 模型供应商实体
 */
@Entity
@Table(name = "providers")
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
     * 供应商类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 32)
    private ProviderType providerType;

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
     * 优先级 (数值越小优先级越高)
     */
    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 自定义 API 端点
     */
    @Column(name = "base_url", length = 256)
    private String baseUrl;
}
