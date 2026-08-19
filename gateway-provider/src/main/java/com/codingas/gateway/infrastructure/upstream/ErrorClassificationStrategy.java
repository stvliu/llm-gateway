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
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.common.enums.ProviderErrorType;

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
