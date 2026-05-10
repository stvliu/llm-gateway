package com.codingas.gateway.application.stats.dto;

/**
 * 系统统计数据响应
 */
public record StatsResponse(
    long providerCount,
    long modelCount,
    long userCount,
    long todayRequests,
    String tokenUsage
) {}
