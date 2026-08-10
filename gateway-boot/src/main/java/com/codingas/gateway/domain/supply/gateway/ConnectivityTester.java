/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;

/**
 * 连通性测试网关接口
 */
public interface ConnectivityTester {

    /**
     * 测试渠道连通性
     *
     * @param channel 渠道实体
     * @return 测试结果
     */
    ConnectivityTestResult test(Channel channel);

    /**
     * 测试指定端点的连通性（用于 Provider 级别测试）
     *
     * @param endpointUrl 端点 URL
     * @param apiKey      API 密钥
     * @param protocol    协议名称（openai / anthropic）
     * @return 测试结果
     */
    default ConnectivityTestResult test(String endpointUrl, String apiKey, String protocol) {
        // 默认实现：构建临时 Channel 委托给 Channel 版本
        Channel tempChannel = new Channel();
        tempChannel.setTimeout(30);
        return test(tempChannel);
    }
}