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
import java.util.List;

/**
 * 上游调用注册表（协议域）
 *
 * <p>按协议收集各插件 {@link ProtocolUpstreamClientFactory}，按协议获取绑定配置的 client。
 * 返回类型擦除为 {@code UpstreamClient<ProtocolRequest>}，调用方无需处理通配符。</p>
 */
public interface UpstreamClientRegistry {

    UpstreamClient<ProtocolRequest> getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds);

    /** 获取系统支持的所有协议标识（来自已装配工厂，新增协议插件自动生效） */
    List<String> getSupportedProtocols();
}
