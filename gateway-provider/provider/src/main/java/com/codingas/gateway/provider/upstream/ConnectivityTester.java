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

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;

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