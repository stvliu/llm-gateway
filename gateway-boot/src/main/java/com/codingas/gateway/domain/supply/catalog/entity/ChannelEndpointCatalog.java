package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点目录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelEndpointCatalog extends BaseEntity {

    /** 所属渠道名 */
    private String channelName;

    /** 协议类型 */
    private Protocol protocol;

    /** 端点 URL */
    private String endpointUrl;
}
