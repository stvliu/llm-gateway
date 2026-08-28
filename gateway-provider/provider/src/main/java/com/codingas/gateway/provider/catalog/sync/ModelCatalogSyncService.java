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

import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 模型目录同步编排服务
 *
 * <p>拉取 models.dev 模型主数据，按 external_id（次选 model_name）幂等 upsert：
 * 新增模型写入完整信息；已有模型由 {@link ModelsDevModelMapper#merge} 更新并跳过
 * 人工锁定字段；modelName 冲突的模型跳过并计入报告。不删除 models.dev 未出现的
 * 现有模型。结果写入 catalog_sync_logs。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCatalogSyncService {

    private final ModelCatalogClient client;
    private final ModelRepository modelRepository;
    private final CatalogSyncLogRepository logRepository;

    /**
     * 执行模型目录同步
     *
     * <p>拉取 models.dev 模型主数据，按 external_id（次选 model_name）幂等 upsert：
     * 新增模型写入完整信息；已有模型由 {@link ModelsDevModelMapper#merge} 更新并跳过
     * 人工锁定字段；modelName 冲突的模型跳过并计入报告。不删除 models.dev 未出现的
     * 现有模型。结果写入 catalog_sync_logs。</p>
     *
     * @return 同步报告
     * @throws CatalogSyncException 拉取失败（已记录 FAILURE 日志）
     */
    @Transactional
    public CatalogSyncReport sync() {
        Instant startedAt = Instant.now();
        CatalogSyncReport report = CatalogSyncReport.builder()
                .success(true)
                .syncedAt(startedAt)
                .messages(new ArrayList<>())
                .build();
        try {
            List<ModelCatalogDto> models = client.fetch();
            for (ModelCatalogDto dto : models) {
                upsertModel(dto, report);
            }
            saveLog(report, null, startedAt);
            log.info("模型目录同步完成: added={}, updated={}, skipped={}, failed={}",
                    report.getAddedCount(), report.getUpdatedCount(),
                    report.getSkippedCount(), report.getFailedCount());
            return report;
        } catch (CatalogSyncException e) {
            saveLog(report, e.getMessage(), startedAt);
            log.error("模型目录同步失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 幂等 upsert 单个目录模型：externalId 命中 → 更新；modelName 命中 →
     * 冲突跳过或补写 externalId 更新；均未命中 → 新增。
     *
     * @param dto    目录模型数据
     * @param report 同步报告（累加计数与消息）
     */
    private void upsertModel(ModelCatalogDto dto, CatalogSyncReport report) {
        Optional<Model> byExternal = modelRepository.findByExternalId(dto.id());
        Optional<Model> byName = byExternal.isPresent() ? Optional.empty()
                : modelRepository.findByModelName(ModelsDevModelMapper.baseModelName(dto.id()));

        if (byExternal.isPresent()) {
            // externalId 命中：合并更新，跳过人工锁定字段
            Model existing = byExternal.get();
            ModelsDevModelMapper.merge(dto, existing);
            modelRepository.save(existing);
            report.incrementUpdated();
        } else if (byName.isPresent()) {
            Model existing = byName.get();
            if (existing.getExternalId() != null && !existing.getExternalId().equals(dto.id())) {
                // modelName 已被其他模型占用：冲突跳过并记录明细
                report.incrementSkipped();
                report.addMessage("modelName 冲突跳过: " + existing.getModelName()
                        + " (externalId=" + existing.getExternalId() + ")");
            } else {
                // 存量模型（无 externalId）：补写 externalId 后合并更新
                existing.setExternalId(dto.id());
                ModelsDevModelMapper.merge(dto, existing);
                modelRepository.save(existing);
                report.incrementUpdated();
            }
        } else {
            // 均未命中：新增模型
            modelRepository.save(ModelsDevModelMapper.toNewModel(dto));
            report.incrementAdded();
        }
    }

    /**
     * 写入同步日志；日志保存失败仅告警，不中断同步主流程。
     *
     * @param report   同步报告
     * @param error    失败原因（成功时为 null）
     * @param syncedAt 同步开始时间
     */
    private void saveLog(CatalogSyncReport report, String error, Instant syncedAt) {
        CatalogSyncLog syncLog = new CatalogSyncLog();
        syncLog.setResult(error == null ? "SUCCESS" : "FAILURE");
        syncLog.setAddedCount(report.getAddedCount());
        syncLog.setUpdatedCount(report.getUpdatedCount());
        syncLog.setSkippedCount(report.getSkippedCount());
        syncLog.setFailedCount(report.getFailedCount());
        syncLog.setMessage(error != null ? error : "同步完成");
        syncLog.setSyncedAt(syncedAt);
        try {
            logRepository.save(syncLog);
        } catch (Exception e) {
            log.warn("保存同步日志失败: {}", e.getMessage());
        }
    }
}
