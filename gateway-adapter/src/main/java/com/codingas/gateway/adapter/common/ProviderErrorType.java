package com.codingas.gateway.adapter.common;

/**
 * 提供商错误类型枚举
 *
 * <p>用于分类 LLM 提供商调用过程中的错误。</p>
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

    /** 网络错误 */
    NETWORK_ERROR,

    /** 未知错误 */
    UNKNOWN_ERROR
}
