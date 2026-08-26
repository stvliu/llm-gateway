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
package com.codingas.gateway.iamdata.application;

import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用根实体数据对象
 *
 * <p>对应 applications 表；主键与审计字段（created_by/created_at/updated_by/updated_at）
 * 继承自 {@link BaseDo}，由 AuditingEntityListener 自动填充。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "applications")
public class ApplicationDo extends BaseDo {

    /** 应用编码，全局唯一 */
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    /** 应用名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 应用描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 应用生命周期状态（ACTIVE/INACTIVE） */
    @Column(name = "state", nullable = false, length = 16)
    private String state;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    @Column(name = "timeout", nullable = false)
    private int timeout;

    /** 应用级失败处理策略（枚举名存储：FAIL_FAST/FAIL_RETRY/FAIL_OVER） */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_strategy", nullable = false, length = 16)
    private FailureStrategy failureStrategy;

    /** 配额预算 ID（预留） */
    @Column(name = "quota_budget_id")
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    @Column(name = "dashboard_id")
    private Long dashboardId;
}
