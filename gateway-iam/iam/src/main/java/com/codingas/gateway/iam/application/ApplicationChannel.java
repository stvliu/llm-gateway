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
package com.codingas.gateway.iam.application;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 应用-渠道授权关联实体
 *
 * <p>应用-渠道授权关联：决定应用可见的渠道集合。</p>
 *
 * <p>模型纯洁：仅含 Getter/Setter，不含业务逻辑；
 * 渠道可见性判定由上层管理服务基于该关联集合完成。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>applicationId — 应用 ID，外键关联 applications.id</li>
 *   <li>channelId — 渠道 ID，外键关联 channels.id</li>
 *   <li>priority — 该应用下该渠道的转移优先级（数值越小越优先，null 回退默认值 100）</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 *
 * <p>唯一约束：(application_id, channel_id) 组合唯一，见 V51 迁移 uk_app_channel。</p>
 *
 * <p>Task 3：转移顺序由全局 ModelInstance.priority 改为应用级 priority，同一渠道对不同应用可有不同转移顺序。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApplicationChannel extends BaseEntity {

    /** 应用 ID */
    private Long applicationId;

    /** 渠道 ID */
    private Long channelId;

    /**
     * 该应用下该渠道的转移优先级（数值越小越优先）
     *
     * <p>为 null 表示未配置，{@code PriorityRouter} 回退默认值 100。
     * 同一渠道在不同应用下可有不同 priority，实现应用级转移顺序。</p>
     */
    private Integer priority;

    /**
     * 业务构造器（仅业务字段）
     *
     * <p>仅初始化业务字段，主键 id 与审计字段（createdBy/createdAt/updatedBy/updatedAt）
     * 继承自 {@link BaseEntity}，由基础设施层在持久化时填充。</p>
     *
     * @param applicationId 应用 ID
     * @param channelId     渠道 ID
     */
    public ApplicationChannel(Long applicationId, Long channelId) {
        this.applicationId = applicationId;
        this.channelId = channelId;
    }
}
