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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.channel.ApiKeyTestResult;
import lombok.Data;

import java.time.Instant;

/**
 * API Key 测试响应 DTO（HTTP 契约）
 */
@Data
public class ApiKeyTestResponse {
    private Boolean success;
    private Long latency;
    private String modelName;
    private String responsePreview;
    private Instant testedAt;
    private ApiKeyTestError error;

    /**
     * 从测试用例结果转换
     *
     * @param result 测试用例结果
     * @return 测试响应 DTO
     */
    public static ApiKeyTestResponse from(ApiKeyTestResult result) {
        ApiKeyTestResponse response = new ApiKeyTestResponse();
        response.setSuccess(result.getSuccess());
        response.setLatency(result.getLatency());
        response.setModelName(result.getModelName());
        response.setResponsePreview(result.getResponsePreview());
        response.setTestedAt(result.getTestedAt());
        if (result.getError() != null) {
            ApiKeyTestError error = new ApiKeyTestError();
            error.setCode(result.getError().getCode());
            error.setMessage(result.getError().getMessage());
            response.setError(error);
        }
        return response;
    }

    /**
     * 测试错误
     */
    @Data
    public static class ApiKeyTestError {
        private String code;
        private String message;
    }
}
