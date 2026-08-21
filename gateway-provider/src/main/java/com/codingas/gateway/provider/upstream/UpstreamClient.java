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

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.provider.upstream.ConnectivityTestResult;

/**
 * 上游调用接口，负责调用上游 LLM API
 *
 * <p>每个实例绑定特定 Provider 配置（endpointUrl/apiKey/timeout），通过 UpstreamClientRegistry 获取。</p>
 */
public interface UpstreamClient {

    /**
     * 非流式调用
     */
    ProtocolResponse chat(ProtocolRequest request);

    /**
     * 流式调用
     */
    void chatStream(ProtocolRequest request, StreamCallback callback);

    /**
     * 连通性测试（测试已绑定 Provider 的连通性）
     */
    ConnectivityTestResult testConnectivity();
}