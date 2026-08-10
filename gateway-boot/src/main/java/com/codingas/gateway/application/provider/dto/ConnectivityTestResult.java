/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
