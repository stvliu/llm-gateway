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
 * 上游客户端工厂（每协议插件注册一个）
 *
 * <p>UpstreamClient 为每请求绑定配置的实例（非单例），注册表收集的是本工厂而非 client 实例。</p>
 */
public interface UpstreamClientFactory {

    /** 支持的协议标识（"openai"/"anthropic"） */
    String supportedProtocol();

    /** 创建绑定指定 Provider 配置的 UpstreamClient */
    UpstreamClient<? extends ProtocolRequest> create(String endpointUrl, String apiKey, int timeoutSeconds);
}
