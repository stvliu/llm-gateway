package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商目录实体（替代 ProviderMetadata）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderCatalog extends BaseEntity {

    private Long providerId;

    /** 供应商代码 */
    private String providerCode;

    private String providerName;

    private String logoUrl;

    private String websiteUrl;

    private String description;

    private MetadataSource source;

    private CatalogState state;
}