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
package com.codingas.gateway.domain.supply.enums;

/**
 * 供应商错误类型枚举
 *
 * <p>用于分类 LLM 供应商调用过程中的错误。</p>
 */
public enum ProviderErrorType {
    /** 认证失败 (API Key 无效/过期) */
    AUTHENTICATION_ERROR,

    /** 限流错误 */
    RATE_LIMIT_ERROR,

    /** 配额超限 */
    QUOTA_EXCEEDED,

    /** 超时错误 */
    TIMEOUT_ERROR,

    /** 请求格式错误 */
    INVALID_REQUEST,

    /** 上游 Provider 错误 */
    UPSTREAM_ERROR,

    /** 上游服务不可用 (503) */
    SERVICE_UNAVAILABLE,

    /** 网络错误 */
    NETWORK_ERROR,

    /** 未知错误 */
    UNKNOWN_ERROR
}