package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 供应商目录实体
 *
 * <p>存储供应商的目录信息，包括厂商标识、类型、默认 API 地址等。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCatalog extends BaseEntity {

    /** 业务标识：openai, volcengine, deepseek */
    private String providerCode;

    /** 展示名 */
    private String providerName;

    /** 供应商类型：INTERNATIONAL / DOMESTIC */
    private ProviderType providerType;

    /** Logo URL */
    private String logoUrl;

    /** 官网 URL */
    private String websiteUrl;

    /** 厂商默认 API 地址 */
    private String baseUrl;

    /** 描述 */
    private String description;

    /** 目录数据来源，默认 BUILTIN */
    private CatalogSource source = CatalogSource.BUILTIN;

    /** 同步时间 */
    private Instant syncedAt;

    /** 目录状态，默认 ACTIVE */
    private CatalogState state = CatalogState.ACTIVE;
}
