/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.supply.dto;

import java.util.List;

/**
 * 健康测试矩阵行（脱敏后对外暴露）
 *
 * @param credentialId    凭证 ID
 * @param keyMasked       脱敏后的 Key（如 sk-***...wxyz）
 * @param auth            认证状态
 * @param authError       失败/超时时的错误说明
 * @param availableModels 可用模型列表
 * @param latencyMs       延迟毫秒数；FAIL/TIMEOUT 时为 null
 */
public record KeyMatrixRow(
        Long credentialId,
        String keyMasked,
        AuthStatus auth,
        String authError,
        List<String> availableModels,
        Long latencyMs
) {
}
