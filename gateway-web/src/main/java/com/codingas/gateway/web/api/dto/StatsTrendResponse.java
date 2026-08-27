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

import com.codingas.gateway.audit.DailyUsage;
import lombok.Data;

/**
 * 调用趋势响应 DTO（按天）
 */
@Data
public class StatsTrendResponse {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 请求数 */
    private long requestCount;

    /** Token 消耗 */
    private long tokenCount;

    /**
     * 从聚合结果转换
     */
    public static StatsTrendResponse from(DailyUsage usage) {
        StatsTrendResponse response = new StatsTrendResponse();
        response.setDate(usage.date());
        response.setRequestCount(usage.requestCount());
        response.setTokenCount(usage.tokenCount());
        return response;
    }
}
