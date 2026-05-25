package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点实体
 *
 * <p>一个 ChannelEndpoint 声明一个协议端点——只回答"用什么协议、调哪个 URL"。</p>
 * <p>一个 Channel 可拥有多个 ChannelEndpoint（如火山引擎 Coding Plan 同时提供 OpenAI 和 Anthropic 两个端点）。</p>
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

    /** 端点状态 */
    private ChannelEndpointState state = ChannelEndpointState.ACTIVE;
}
