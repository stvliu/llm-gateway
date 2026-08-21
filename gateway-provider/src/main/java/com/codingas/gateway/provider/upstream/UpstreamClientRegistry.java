/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.upstream;

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