/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 渠道端点响应
 */
@Data
public class ChannelEndpointResponse {

    private Long id;

    private Long channelId;

    private String protocol;

    private String endpointUrl;

    private Instant createdAt;

    private Instant updatedAt;
}
