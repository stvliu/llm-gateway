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
package com.codingas.gateway.domain.supply.gateway;

/**
 * 韧性客户端工厂
 *
 * <p>为原始 UpstreamClient 包装熔断器 + 重试策略，返回带韧性保护的 UpstreamClient。</p>
 * <p>接口定义在 domain 层，实现在 infrastructure 层，避免 application 层直接依赖 infrastructure 实现类。</p>
 */
public interface ResilientClientFactory {

    /**
     * 为原始 UpstreamClient 包装韧性保护
     *
     * @param rawClient       原始上游客户端
     * @param channelEndpointId 端点 ID（用于获取对应的熔断器）
     * @return 带韧性保护的 UpstreamClient
     */
    UpstreamClient wrap(UpstreamClient rawClient, Long channelEndpointId);
}