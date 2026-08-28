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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 模型目录同步日志实体
 *
 * <p>记录一次模型目录同步（models.dev）的执行结果，用于审计与失败排查。
 * 对应 catalog_sync_logs 表。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CatalogSyncLog extends BaseEntity {

    /** 触发人：SYSTEM（定时）/ 用户登录名（手动触发） */
    private String triggeredBy;

    /** 同步结果：SUCCESS / FAILURE */
    private String result;

    /** 本次新增模型数 */
    private int addedCount;

    /** 本次更新模型数 */
    private int updatedCount;

    /** 本次跳过模型数 */
    private int skippedCount;

    /** 本次失败模型数 */
    private int failedCount;

    /** 同步结果描述（成功摘要 / 失败原因） */
    private String message;

    /** 同步完成时间 */
    private Instant syncedAt;
}
