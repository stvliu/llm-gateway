package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 供应商目录实体
 *
 * <p>存储供应商的目录信息，包括厂商标识、默认 API 地址等。</p>
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

    /** Logo URL */
    private String logoUrl;

    /** 官网 URL */
    private String websiteUrl;

    /** 描述 */
    private String description;

    /** 同步时间 */
    private Instant syncedAt;

    /** 目录状态，默认 ACTIVE */
    private CatalogState state = CatalogState.ACTIVE;
}
