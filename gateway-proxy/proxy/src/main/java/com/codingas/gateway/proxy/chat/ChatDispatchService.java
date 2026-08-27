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
package com.codingas.gateway.proxy.chat;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.proxy.routing.RoutingStrategy;
import com.codingas.gateway.iam.auth.Identity;

/**
 * 聊天调度服务
 *
 * <p>七阶段调用链编排：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 */
public interface ChatDispatchService {

    /**
     * 非流式调度
     *
     * @param request 协议请求
     * @param identity 认证结果
     * @param strategy 路由策略
     * @return 协议响应
     */
    ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy);

    /**
     * 流式调度
     *
     * @param request 协议请求
     * @param identity 认证结果
     * @param strategy 路由策略
     * @param callback 流式回调
     */
    void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                        StreamCallback callback);
}