/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.stats.dto;

/**
 * 系统统计数据响应
 */
public record StatsResponse(
    long providerCount,
    long channelCount,
    long modelCount,
    long userCount,
    long todayRequests,
    String tokenUsage
) {}
