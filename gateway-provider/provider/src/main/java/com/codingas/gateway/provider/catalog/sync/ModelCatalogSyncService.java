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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 *
 * <p>事务边界：仅模型 upsert 循环运行在独立事务内（{@link TransactionTemplate}）；
 * 拉取与同步日志写入均在事务之外，保证失败场景下 FAILURE 日志能独立落库，
 * 不被事务回滚。</p>
 */
@Slf4j
@Service
public class ModelCatalogSyncService {

    private final ModelCatalogClient client;
    private final ModelRepository modelRepository;
    private final CatalogSyncLogRepository logRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造同步编排服务
     *
     * <p>由 {@link PlatformTransactionManager} 构建独立 {@link TransactionTemplate}，
     * 用于包裹模型 upsert 的事务边界（事务外拉取与写日志）。</p>
     *
     * @param client             models.dev 目录数据源客户端
     * @param modelRepository    模型持久化接口
     * @param logRepository      同步日志持久化接口
     * @param transactionManager 事务管理器
     */
    public ModelCatalogSyncService(ModelCatalogClient client,
                                   ModelRepository modelRepository,
                                   CatalogSyncLogRepository logRepository,
                                   PlatformTransactionManager transactionManager) {
        this.client = client;
        this.modelRepository = modelRepository;
        this.logRepository = logRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 执行模型目录同步
     *
     * <p>拉取（事务外）→ 模型幂等 upsert（独立事务内，整体成功或回滚）→ 写 SUCCESS 日志
     * （事务外）→ 返回报告。任意 {@link RuntimeException}（含拉取失败与 upsert 异常）
     * 都会先记录 FAILURE 日志（事务外独立落库，不被回滚）后再抛出。模型 upsert 幂等，
     * 部分失败后重跑可恢复。</p>
     *
     * @return 同步报告
     * @throws CatalogSyncException 拉取失败（已记录 FAILURE 日志）
     */
    public CatalogSyncReport sync() {
        Instant startedAt = Instant.now();
        CatalogSyncReport report = CatalogSyncReport.builder()
                .success(true)
                .syncedAt(startedAt)
                .messages(new ArrayList<>())
                .build();
        try {
            // 事务外拉取：拉取失败不影响 FAILURE 日志独立落库
            List<ModelCatalogDto> models = client.fetch();
            // 事务内模型 upsert：全部成功或整体回滚，重跑可恢复
            transactionTemplate.executeWithoutResult(status -> {
                for (ModelCatalogDto dto : models) {
                    upsertModel(dto, report);
                }
            });
            saveLog(report, null, startedAt);
            log.info("模型目录同步完成: added={}, updated={}, skipped={}, failed={}",
                    report.getAddedCount(), report.getUpdatedCount(),
                    report.getSkippedCount(), report.getFailedCount());
            return report;
        } catch (RuntimeException e) {
            // 事务外记录 FAILURE 日志（独立落库、失败仅告警），再抛出异常
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
                // 存量模型（无 externalId）：补写 externalId 并接管为 models.dev 数据源后合并更新
                existing.setExternalId(dto.id());
                existing.setSource(ModelsDevModelMapper.SOURCE);
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
