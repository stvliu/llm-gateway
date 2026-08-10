/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
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
