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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.upstream.KeyTestResult;

/**
 * 单 Key 连通性探针接口（最小占位）
 *
 * <p>占位实现：第 9 章会接入真实的出站连通性测试与模型探测；当前默认 {@code Stub} 实现返回 PASS。</p>
 *
 * <p>与既有 {@link ConnectivityTester} 的差异：</p>
 * <ul>
 *   <li>{@code ConnectivityTester} 面向 Channel 整体，仅返回布尔型连通性结果</li>
 *   <li>{@code ChannelKeyProbe} 面向单条凭证，返回 {@link KeyTestResult}（含可用模型列表、延迟、认证状态）</li>
 * </ul>
 */
public interface ChannelKeyProbe {

    /**
     * 对指定渠道下的某一条凭证发起连通性测试
     *
     * @param channel    渠道实体
     * @param credential 凭证实体
     * @return 单 Key 测试结果
     */
    KeyTestResult test(Channel channel, ChannelCredential credential);
}
