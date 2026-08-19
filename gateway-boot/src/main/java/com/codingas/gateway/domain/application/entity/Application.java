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
package com.codingas.gateway.domain.application.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.enums.FailureStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 应用聚合根实体
 *
 * <p>应用聚合根：权限+行为双聚合，承载 Key 归属、渠道可见性、应用级超时，
 * 预留配额/看板字段。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑；
 * 路由判定下沉至 {@link ApplicationState#isRoutable()}。</p>
 *
 * <p>Task 8：{@code resilienceProfileId} 退场（ResilienceProfile 实体删除），
 * {@code timeout} 直接挂 Application 字段，承接原 ResilienceProfile.timeout 语义。</p>
 *
 * <p>Task 5：新增 {@code failureStrategy} 字段（FAIL_FAST/FAIL_RETRY/FAIL_OVER），
 * 默认 FAIL_RETRY，承接原 ResilienceProfile 的失败处理策略语义。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>code — 应用编码，全局唯一</li>
 *   <li>name — 应用名称</li>
 *   <li>description — 应用描述</li>
 *   <li>state — 应用生命周期状态，控制是否可路由</li>
 *   <li>timeout — 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout）</li>
 *   <li>failureStrategy — 应用级失败处理策略（默认 FAIL_RETRY）</li>
 *   <li>quotaBudgetId — 配额预算 ID（预留，后续任务填充）</li>
 *   <li>dashboardId — 看板 ID（预留，后续任务填充）</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class Application extends BaseEntity {

    /** 应用编码，全局唯一 */
    private String code;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 应用生命周期状态 */
    private ApplicationState state;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    private int timeout;

    /** 应用级失败处理策略（默认 FAIL_RETRY） */
    private FailureStrategy failureStrategy;

    /** 配额预算 ID（预留） */
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    private Long dashboardId;

    /**
     * 全参构造器（仅业务字段）
     *
     * <p>仅初始化业务字段，主键 id 与审计字段（createdBy/createdAt/updatedBy/updatedAt）
     * 继承自 {@link BaseEntity}，由基础设施层在持久化时填充。</p>
     *
     * @param code            应用编码
     * @param name            应用名称
     * @param description     应用描述
     * @param state           应用生命周期状态
     * @param timeout         请求超时秒数（0 表示用渠道默认）
     * @param failureStrategy 应用级失败处理策略（默认 FAIL_RETRY）
     * @param quotaBudgetId   配额预算 ID
     * @param dashboardId     看板 ID
     */
    public Application(String code, String name, String description, ApplicationState state,
                       int timeout, FailureStrategy failureStrategy,
                       Long quotaBudgetId, Long dashboardId) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.state = state;
        this.timeout = timeout;
        this.failureStrategy = failureStrategy;
        this.quotaBudgetId = quotaBudgetId;
        this.dashboardId = dashboardId;
    }
}
