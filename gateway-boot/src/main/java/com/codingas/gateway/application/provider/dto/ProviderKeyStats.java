package com.codingas.gateway.application.provider.dto;

/**
 * Provider Key 统计信息
 *
 * <p>用于在 Provider 列表中展示 Key 的数量统计。</p>
 */
public record ProviderKeyStats(
    Long providerId,
    int totalCount,
    int activeCount
) {}