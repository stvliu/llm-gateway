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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 协议域注册表实现
 *
 * <p>注入全部 {@link UpstreamClientFactory} Bean，按 supportedProtocol() 建立索引；
 * getClient 选择工厂创建实例并擦除泛型为基类，调用方无感。</p>
 */
@Component
public class UpstreamClientRegistryImpl implements UpstreamClientRegistry {

    private final Map<String, UpstreamClientFactory> factories;

    public UpstreamClientRegistryImpl(List<UpstreamClientFactory> factoryList) {
        // LinkedHashMap 保持工厂装配顺序，getSupportedProtocols 返回稳定（可观测行为）
        this.factories = factoryList.stream()
                .collect(Collectors.toMap(UpstreamClientFactory::supportedProtocol, Function.identity(),
                        (existing, replacement) -> existing, LinkedHashMap::new));
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpstreamClient<ProtocolRequest> getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds) {
        UpstreamClientFactory factory = factories.get(protocol);
        if (factory == null) {
            throw new IllegalArgumentException("不支持的协议: " + protocol);
        }
        return (UpstreamClient<ProtocolRequest>) factory.create(endpointUrl, apiKey, timeoutSeconds);
    }

    @Override
    public List<String> getSupportedProtocols() {
        return new ArrayList<>(factories.keySet());
    }
}
