/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.valueobject;

/**
 * 连通性测试结果值对象
 *
 * <p>用于协议网关的连通性测试返回。</p>
 */
public record ConnectivityTestResult(
        boolean success,
        Long channelId,
        String errorMessage,
        long latencyMs
) {

    /**
     * 创建成功结果
     */
    public static ConnectivityTestResult success(Long channelId, long latencyMs) {
        return new ConnectivityTestResult(true, channelId, null, latencyMs);
    }

    /**
     * 创建失败结果
     */
    public static ConnectivityTestResult failure(Long channelId, String errorMessage) {
        return new ConnectivityTestResult(false, channelId, errorMessage, 0);
    }
}