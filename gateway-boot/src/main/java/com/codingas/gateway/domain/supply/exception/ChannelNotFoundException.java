package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 渠道不存在异常
 */
public class ChannelNotFoundException extends GatewayException {

    public ChannelNotFoundException(Long channelId) {
        super("CHANNEL_NOT_FOUND", "渠道不存在: " + channelId);
    }
}
