package com.codingas.gateway.infrastructure.supply.catalog.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 供应商目录数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "provider_catalogs", uniqueConstraints = @UniqueConstraint(columnNames = "provider_code"))
public class ProviderCatalogDo extends BaseDo {

    @Column(name = "provider_code", nullable = false, unique = true, length = 64)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}
