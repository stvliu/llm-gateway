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
package com.codingas.gateway.application.application.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 应用响应 DTO
 *
 * <p>返回应用聚合根的完整字段，含应用级 timeout 与预留的配额预算/看板 ID。</p>
 *
 * <p>Task 8：{@code resilienceProfileId} 退场，改为 {@code timeout}（承接原 ResilienceProfile.timeout）。</p>
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
}
