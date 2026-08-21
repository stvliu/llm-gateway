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

/**
 * 连通性测试结果值对象
 *
 * <p>用于协议网关的连通性测试返回。</p>
 */
public record ConnectivityTestResult(
        boolean success,
        Long channelId,
        String errorMessage,
        long latencyMs
) {

    /**
     * 创建成功结果
     */
    public static ConnectivityTestResult success(Long channelId, long latencyMs) {
        return new ConnectivityTestResult(true, channelId, null, latencyMs);
    }

    /**
     * 创建失败结果
     */
    public static ConnectivityTestResult failure(Long channelId, String errorMessage) {
        return new ConnectivityTestResult(false, channelId, errorMessage, 0);
    }
}