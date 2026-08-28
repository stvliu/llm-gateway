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

import com.codingas.gateway.provider.catalog.sync.CatalogSyncLog;
import com.codingas.gateway.provider.catalog.sync.CatalogSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 模型目录同步日志持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JpaCatalogSyncLogRepository implements CatalogSyncLogRepository {

    private final CatalogSyncLogJpaRepository jpaRepository;

    @Override
    public CatalogSyncLog save(CatalogSyncLog log) {
        CatalogSyncLogDo saved = jpaRepository.save(toDo(log));
        return toEntity(saved);
    }

    @Override
    public Optional<CatalogSyncLog> findLatest() {
        return jpaRepository.findTopByOrderBySyncedAtDesc().map(this::toEntity);
    }

    /**
     * DO → 领域实体转换（计数缺省按 0 兜底，避免 NPE）
     */
    private CatalogSyncLog toEntity(CatalogSyncLogDo doObj) {
        CatalogSyncLog entity = new CatalogSyncLog();
        entity.setId(doObj.getId());
        entity.setTriggeredBy(doObj.getTriggeredBy());
        entity.setResult(doObj.getResult());
        entity.setAddedCount(nvl(doObj.getAddedCount()));
        entity.setUpdatedCount(nvl(doObj.getUpdatedCount()));
        entity.setSkippedCount(nvl(doObj.getSkippedCount()));
        entity.setFailedCount(nvl(doObj.getFailedCount()));
        entity.setMessage(doObj.getMessage());
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    /**
     * 领域实体 → DO 转换
     */
    private CatalogSyncLogDo toDo(CatalogSyncLog entity) {
        CatalogSyncLogDo doObj = new CatalogSyncLogDo();
        doObj.setId(entity.getId());
        doObj.setTriggeredBy(entity.getTriggeredBy());
        doObj.setResult(entity.getResult());
        doObj.setAddedCount(entity.getAddedCount());
        doObj.setUpdatedCount(entity.getUpdatedCount());
        doObj.setSkippedCount(entity.getSkippedCount());
        doObj.setFailedCount(entity.getFailedCount());
        doObj.setMessage(entity.getMessage());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }

    /**
     * 空值兜底：数据库计数列缺省为 0
     */
    private int nvl(Integer value) {
        return value != null ? value : 0;
    }
}
