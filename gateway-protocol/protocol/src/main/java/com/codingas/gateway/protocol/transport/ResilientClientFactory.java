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

/**
 * 韧性客户端工厂（协议域端口，resilience 域提供实现）
 *
 * <p>为原始 UpstreamClient 包装熔断 + 重试等韧性保护。</p>
 */
public interface ResilientClientFactory {

    UpstreamClient<ProtocolRequest> wrap(UpstreamClient<ProtocolRequest> rawClient, Long channelEndpointId);
}
