package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道目录实体（替代 ProductMetadata）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelCatalog extends BaseEntity {

    private Long channelId;

    private String channelName;

    private String providerCode;

    private CatalogState state;
}