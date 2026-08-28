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
package com.codingas.gateway.web.api;

import com.codingas.gateway.provider.catalog.sync.CatalogSyncLogRepository;
import com.codingas.gateway.provider.catalog.sync.ModelCatalogSyncService;
import com.codingas.gateway.web.api.dto.CatalogSyncReportResponse;
import com.codingas.gateway.web.api.dto.CatalogSyncStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型目录同步控制器
 *
 * <p>提供模型目录（models.dev）同步的手工触发与最近一次同步状态查询端点。</p>
 */
@RestController
@RequestMapping("/api/v1/catalog/sync")
@RequiredArgsConstructor
public class CatalogSyncController {

    private final ModelCatalogSyncService syncService;
    private final CatalogSyncLogRepository logRepository;

    /**
     * 手工触发模型目录同步
     *
     * @return 同步报告（新增/更新/跳过/失败计数与消息明细）
     */
    @PostMapping
    public CatalogSyncReportResponse sync() {
        return CatalogSyncReportResponse.from(syncService.sync());
    }

    /**
     * 查询最近一次同步状态
     *
     * @return 最近同步记录；无记录时返回 204 无内容
     */
    @GetMapping("/status")
    public ResponseEntity<CatalogSyncStatusResponse> status() {
        return logRepository.findLatest()
                .map(l -> ResponseEntity.ok(CatalogSyncStatusResponse.from(l)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
