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

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 同步日志 JPA Repository
 */
public interface CatalogSyncLogJpaRepository extends JpaRepository<CatalogSyncLogDo, Long> {

    /**
     * 查询同步时间最新的一条日志（按 synced_at 倒序取首条）
     *
     * @return 最新同步日志，无记录时返回空
     */
    Optional<CatalogSyncLogDo> findTopByOrderBySyncedAtDesc();

    /**
     * 按触发来源查询该来源最新的一条日志（按 synced_at 倒序取首条）
     *
     * @param triggeredBy 触发来源（SYNC=目录同步 / PROBE=上游探测）
     * @return 指定来源的最新日志，无记录时返回空
     */
    Optional<CatalogSyncLogDo> findTopByTriggeredByOrderBySyncedAtDesc(String triggeredBy);
}
