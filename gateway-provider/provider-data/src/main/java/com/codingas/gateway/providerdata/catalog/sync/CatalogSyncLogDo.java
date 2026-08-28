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
package com.codingas.gateway.providerdata.catalog.sync;

import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 模型目录同步日志数据对象
 *
 * <p>映射 catalog_sync_logs 表，与领域实体 {@link com.codingas.gateway.provider.catalog.sync.CatalogSyncLog}
 * 一一对应。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "catalog_sync_logs")
public class CatalogSyncLogDo extends BaseDo {

    /** 触发人：SYSTEM（定时）/ 用户登录名（手动触发） */
    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    /** 同步结果：SUCCESS / FAILURE */
    @Column(name = "result", nullable = false, length = 16)
    private String result;

    /** 本次新增模型数 */
    @Column(name = "added_count")
    private Integer addedCount;

    /** 本次更新模型数 */
    @Column(name = "updated_count")
    private Integer updatedCount;

    /** 本次跳过模型数 */
    @Column(name = "skipped_count")
    private Integer skippedCount;

    /** 本次失败模型数 */
    @Column(name = "failed_count")
    private Integer failedCount;

    /** 同步结果描述（成功摘要 / 失败原因） */
    @Column(name = "message", columnDefinition = "text")
    private String message;

    /** 同步完成时间 */
    @Column(name = "synced_at")
    private Instant syncedAt;
}
