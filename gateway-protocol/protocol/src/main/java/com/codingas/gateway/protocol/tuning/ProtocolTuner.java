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
package com.codingas.gateway.protocol.tuning;

import com.codingas.gateway.protocol.ProtocolRequest;

/**
 * 协议级出站调谐器
 *
 * <p>在请求发送到上游之前，对请求做协议特定的默认值补全和规范化。
 * 例如：OpenAI 补 max_tokens、Anthropic 提取 system 角色消息。</p>
 *
 * <p>注意：协议级调谐只做默认值补全，不做模型名替换。
 * 模型名替换由应用层的 OutboundTuner 编排处理。</p>
 */
public interface ProtocolTuner<T extends ProtocolRequest> {

    /**
     * 获取支持的协议标识
     */
    String getProtocol();

    /**
     * 对请求做协议级调谐
     *
     * @param request 原始请求
     * @return 调谐后的请求（可能修改了默认值）
     */
    T tune(T request);
}