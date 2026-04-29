package com.codingas.gateway.infrastructure.router;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.common.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 提供商 DO
 *
 * <p>JPA 实体，对应数据库 providers 表。</p>
 */
@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDo extends BaseDo {

    @Column(name = "provider_code", nullable = false, unique = true, length = 64)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Column(name = "base_url", length = 256)
    private String baseUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "api_doc_url", length = 512)
    private String apiDocUrl;

    @Column(name = "priority")
    private Integer priority = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProviderStatus status = ProviderStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ProviderStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}
