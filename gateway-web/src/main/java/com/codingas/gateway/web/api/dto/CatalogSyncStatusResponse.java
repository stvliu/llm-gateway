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

import com.codingas.gateway.provider.catalog.sync.CatalogSyncLog;
import lombok.Data;

import java.time.Instant;

/**
 * 模型目录同步状态响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(CatalogSyncLog)} 从最近一次同步日志生成，对应
 * 同步状态查询端点的返回体。</p>
 */
@Data
public class CatalogSyncStatusResponse {
    /** 同步结果：SUCCESS / FAILURE */
    private String result;

    /** 新增模型数 */
    private int addedCount;

    /** 更新模型数 */
    private int updatedCount;

    /** 跳过模型数 */
    private int skippedCount;

    /** 失败模型数 */
    private int failedCount;

    /** 同步结果描述 */
    private String message;

    /** 同步完成时间 */
    private Instant syncedAt;

    /**
     * 从同步日志实体转换
     *
     * @param log 同步日志实体
     * @return 同步状态响应 DTO
     */
    public static CatalogSyncStatusResponse from(CatalogSyncLog log) {
        CatalogSyncStatusResponse response = new CatalogSyncStatusResponse();
        response.setResult(log.getResult());
        response.setAddedCount(log.getAddedCount());
        response.setUpdatedCount(log.getUpdatedCount());
        response.setSkippedCount(log.getSkippedCount());
        response.setFailedCount(log.getFailedCount());
        response.setMessage(log.getMessage());
        response.setSyncedAt(log.getSyncedAt());
        return response;
    }
}
