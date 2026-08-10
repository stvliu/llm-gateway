/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

import java.util.List;

/**
 * 上游调用注册表，按协议类型获取绑定 Provider 配置的 UpstreamClient 实例
 */
public interface UpstreamClientRegistry {

    /**
     * 获取绑定特定 Provider 配置的 UpstreamClient 实例
     *
     * @param protocol       协议标识（"openai" / "anthropic"）
     * @param endpointUrl    上游 Endpoint URL
     * @param apiKey         上游 API Key
     * @param timeoutSeconds 超时秒数
     * @return 绑定配置的 UpstreamClient 实例
     */
    UpstreamClient getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds);

    /**
     * 获取系统支持的所有协议标识
     */
    List<String> getSupportedProtocols();
}