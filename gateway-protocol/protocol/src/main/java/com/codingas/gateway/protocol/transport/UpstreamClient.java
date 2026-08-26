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
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;

/**
 * 上游调用接口（协议传输端口）
 *
 * <p>每个实例绑定特定 Provider 配置（endpointUrl/apiKey/timeout），
 * 由 {@link UpstreamClientFactory} 创建、{@link UpstreamClientRegistry} 按协议获取。</p>
 *
 * @param <T> 该客户端专署的协议请求类型
 */
public interface UpstreamClient<T extends ProtocolRequest> {

    /** 非流式调用 */
    ProtocolResponse chat(T request);

    /** 流式调用 */
    void chatStream(T request, StreamCallback callback);

    /** 连通性测试（测试已绑定 Provider 的连通性） */
    ConnectivityTestResult testConnectivity();

    /** 协议标识自描述（"openai"/"anthropic"），供韧性层按协议归类，替代对实现的 instanceof */
    String supportedProvider();
}
