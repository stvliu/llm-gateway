package com.codingas.gateway.domain.supply.entity;

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
@DomainEntity
public class ChannelEndpoint extends BaseEntity {

    /** 所属渠道 ID */
    private Long channelId;

    /** 协议类型 */
    private Protocol protocol;

    /** 端点 URL */
    private String endpointUrl;

    /** 端点状态 */
    private ChannelEndpointState state = ChannelEndpointState.ACTIVE;

    /**
     * 判断端点是否可用
     */
    public boolean isAvailable() {
        return ChannelEndpointState.ACTIVE.equals(state);
    }

    /**
     * 禁用端点
     */
    public void disable() {
        this.state = ChannelEndpointState.INACTIVE;
    }

    /**
     * 启用端点
     */
    public void enable() {
        this.state = ChannelEndpointState.ACTIVE;
    }
}
