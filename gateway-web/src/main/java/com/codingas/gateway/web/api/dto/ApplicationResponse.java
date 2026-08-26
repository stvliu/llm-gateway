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

import com.codingas.gateway.iam.application.Application;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 应用响应 DTO（HTTP 契约）
 *
 * <p>返回应用根实体的完整字段，含应用级 timeout 与预留的配额预算/看板 ID。</p>
 */
@Data
public class ApplicationResponse {

    /** 应用 ID */
    private Long id;

    /** 应用编码，全局唯一 */
    private String code;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 应用生命周期状态（ACTIVE/INACTIVE） */
    private String state;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    private int timeout;

    /** 应用级失败处理策略（FAIL_FAST/FAIL_RETRY/FAIL_OVER） */
    private String failureStrategy;

    /** 配额预算 ID（预留） */
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    private Long dashboardId;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /**
     * 从应用实体转换
     *
     * @param app 应用实体
     * @return 应用响应 DTO
     */
    public static ApplicationResponse from(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getId());
        response.setCode(app.getCode());
        response.setName(app.getName());
        response.setDescription(app.getDescription());
        response.setState(app.getState() != null ? app.getState().name() : null);
        response.setTimeout(app.getTimeout());
        response.setFailureStrategy(app.getFailureStrategy() != null
                ? app.getFailureStrategy().name() : null);
        response.setQuotaBudgetId(app.getQuotaBudgetId());
        response.setDashboardId(app.getDashboardId());
        response.setCreatedAt(app.getCreatedAt());
        response.setUpdatedAt(app.getUpdatedAt());
        return response;
    }

    /**
     * 从应用实体列表转换
     *
     * @param apps 应用实体列表
     * @return 应用响应 DTO 列表
     */
    public static List<ApplicationResponse> from(List<Application> apps) {
        return apps.stream().map(ApplicationResponse::from).toList();
    }
}
