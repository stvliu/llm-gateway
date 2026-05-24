package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.MetadataSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 渠道模型目录实体（替代 ProductModelMetadata）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelModelCatalog extends BaseEntity {

    private Long channelModelId;

    private String providerModelId;

    private String channelName;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private MetadataSource source;

    private CatalogState state;
}