package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 错误分类策略接口
 *
 * <p>根据 HTTP 状态码和响应体内容，将上游错误映射为 ProviderErrorType。</p>
 */
public interface ErrorClassificationStrategy {

    /**
     * 分类上游错误
     *
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体（可为 null）
     * @return 映射后的错误类型
     */
    ProviderErrorType classify(int statusCode, String responseBody);

    /**
     * 获取此策略支持的 Provider 名称
     */
    String supportedProvider();
}
