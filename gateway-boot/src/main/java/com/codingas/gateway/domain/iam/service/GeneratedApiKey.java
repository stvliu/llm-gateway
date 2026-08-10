/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.service;

/**
 * API Key 生成结果值对象
 *
 * @param plainKey  完整明文 Key（仅创建时展示一次）
 * @param keyPrefix Key 前缀（用于数据库索引查找）
 */
public record GeneratedApiKey(String plainKey, String keyPrefix) {
}
