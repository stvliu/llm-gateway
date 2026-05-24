package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 模型目录实体（替代 ModelMetadata）
 *
 * <p>纯属性实体，不持有定价和渠道关联信息。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelCatalog extends BaseEntity {

    private String providerModelId;

    private String displayName;

    private String modelFamily;

    private String providerCode;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private MetadataSource source;

    private CatalogState state;
}