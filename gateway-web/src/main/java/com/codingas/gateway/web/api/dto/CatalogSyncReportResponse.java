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

import com.codingas.gateway.provider.catalog.sync.CatalogSyncReport;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 模型目录同步报告响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(CatalogSyncReport)} 从同步报告生成，对应手工触发
 * 同步端点的返回体。</p>
 */
@Data
public class CatalogSyncReportResponse {
    /** 同步是否成功 */
    private boolean success;

    /** 新增模型数 */
    private int addedCount;

    /** 更新模型数 */
    private int updatedCount;

    /** 跳过模型数 */
    private int skippedCount;

    /** 失败模型数 */
    private int failedCount;

    /** 同步消息明细 */
    private List<String> messages;

    /** 同步开始时间 */
    private Instant syncedAt;

    /**
     * 从同步报告转换
     *
     * @param report 同步报告
     * @return 同步报告响应 DTO
     */
    public static CatalogSyncReportResponse from(CatalogSyncReport report) {
        CatalogSyncReportResponse response = new CatalogSyncReportResponse();
        response.setSuccess(report.isSuccess());
        response.setAddedCount(report.getAddedCount());
        response.setUpdatedCount(report.getUpdatedCount());
        response.setSkippedCount(report.getSkippedCount());
        response.setFailedCount(report.getFailedCount());
        response.setMessages(report.getMessages());
        response.setSyncedAt(report.getSyncedAt());
        return response;
    }
}
