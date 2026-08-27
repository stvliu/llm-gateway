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

import com.codingas.gateway.audit.ModelUsage;
import lombok.Data;

/**
 * 模型用量分布响应 DTO
 */
@Data
public class StatsModelUsageResponse {

    /** 模型标识 */
    private String model;

    /** 请求数 */
    private long requestCount;

    /**
     * 从聚合结果转换
     */
    public static StatsModelUsageResponse from(ModelUsage usage) {
        StatsModelUsageResponse response = new StatsModelUsageResponse();
        response.setModel(usage.model());
        response.setRequestCount(usage.requestCount());
        return response;
    }
}
