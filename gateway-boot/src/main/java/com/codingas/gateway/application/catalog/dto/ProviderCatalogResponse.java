package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 供应商目录响应
 */
@Getter
@Builder
public class ProviderCatalogResponse {

    /** 供应商编码 */
    private final String code;

    /** 供应商名称 */
    private final String name;

    /** 供应商类型 */
    private final String providerType;

    /** 数据来源 */
    private final String source;

    /** 是否已物化 */
    private final Boolean materialized;
}
