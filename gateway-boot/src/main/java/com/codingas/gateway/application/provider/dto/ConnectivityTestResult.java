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
package com.codingas.gateway.application.provider.dto;

import java.util.List;

/**
 * 连通性测试结果
 *
 * @param success         整体是否成功
 * @param message         摘要消息
 * @param models          发现的模型列表
 * @param level1          Level 1 测试结果（认证）
 * @param level2          Level 2 测试结果（模型可用性）
 * @param totalLatencyMs  总耗时（毫秒）
 */
public record ConnectivityTestResult(
    boolean success,
    String message,
    List<String> models,
    LevelResult level1,
    LevelResult level2,
    long totalLatencyMs
) {
    /**
     * 分层测试结果
     *
     * @param success    是否成功
     * @param message    结果消息
     * @param latencyMs  响应延迟（毫秒）
     * @param errorType  错误分类（ProviderErrorType 名称），成功时为 null
     * @param models     Level 1 特有：发现的模型列表
     */
    public record LevelResult(
        boolean success,
        String message,
        Long latencyMs,
        String errorType,
        List<String> models
    ) {}
}
