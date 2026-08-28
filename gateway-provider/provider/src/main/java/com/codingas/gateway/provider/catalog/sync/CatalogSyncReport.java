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

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 模型目录同步报告
 *
 * <p>一次模型目录同步（models.dev）的执行结果汇总：成功标识、各操作计数、
 * 冲突/异常消息明细与同步时间。由 {@link ModelCatalogSyncService} 生产并返回。</p>
 */
@Data
@Builder
public class CatalogSyncReport {

    /** 同步是否成功 */
    private boolean success;

    /** 新增模型数 */
    private int addedCount;

    /** 更新模型数 */
    private int updatedCount;

    /** 跳过模型数（modelName 冲突等） */
    private int skippedCount;

    /** 失败模型数 */
    private int failedCount;

    /** 同步消息明细（冲突跳过、异常等） */
    private List<String> messages;

    /** 同步开始时间 */
    private Instant syncedAt;

    /**
     * 新增计数 +1
     */
    public void incrementAdded() {
        this.addedCount++;
    }

    /**
     * 更新计数 +1
     */
    public void incrementUpdated() {
        this.updatedCount++;
    }

    /**
     * 跳过计数 +1
     */
    public void incrementSkipped() {
        this.skippedCount++;
    }

    /**
     * 失败计数 +1
     */
    public void incrementFailed() {
        this.failedCount++;
    }

    /**
     * 追加一条同步消息
     *
     * @param message 消息内容
     */
    public void addMessage(String message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
}
