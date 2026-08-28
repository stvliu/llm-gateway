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

import java.util.Optional;

/**
 * 模型目录同步日志 Repository（领域接口）
 *
 * <p>负责同步日志的持久化与最近一次同步记录查询。</p>
 */
public interface CatalogSyncLogRepository {

    /**
     * 保存一条同步日志
     *
     * @param log 同步日志实体
     * @return 保存后的同步日志（携带数据库生成字段）
     */
    CatalogSyncLog save(CatalogSyncLog log);

    /**
     * 查询最近一次同步日志（按同步时间倒序取最新一条）
     *
     * @return 最近一次同步日志，无记录时返回空
     */
    Optional<CatalogSyncLog> findLatest();
}
