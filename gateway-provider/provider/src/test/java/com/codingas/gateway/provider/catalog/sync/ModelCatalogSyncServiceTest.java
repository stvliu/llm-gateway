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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelCatalogSyncService 单元测试
 *
 * <p>验证模型目录同步编排：新增/更新/跳过统计、modelName 冲突跳过、
 * 存量模型补写 externalId、拉取失败记录 FAILURE 日志并抛异常。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelCatalogSyncServiceTest {

    @Mock private ModelCatalogClient client;
    @Mock private ModelRepository modelRepository;
    @Mock private CatalogSyncLogRepository logRepository;

    private ModelCatalogSyncService service;

    private final ModelCatalogDto gpt4o = new ModelCatalogDto(
            "openai/gpt-4o", "GPT-4o", "desc", "gpt",
            true, false, true, true, true, "2023-10",
            LocalDate.of(2024, 5, 13), LocalDate.of(2026, 8, 1), false,
            new ModelCatalogDto.ModalitiesDto(List.of("text", "image"), List.of("text")),
            new ModelCatalogDto.LimitDto(128000L, 16384L, 128000L),
            "Proprietary", List.of(), List.of());

    private final ModelCatalogDto deepseek = new ModelCatalogDto(
            "deepseek/deepseek-v4-flash", "DeepSeek V4 Flash", "desc2", "deepseek-flash",
            false, true, true, false, true, "2025-12",
            LocalDate.of(2026, 4, 24), LocalDate.of(2026, 8, 10), true,
            new ModelCatalogDto.ModalitiesDto(List.of("text"), List.of("text")),
            new ModelCatalogDto.LimitDto(1000000L, 128000L, 1000000L),
            "MIT", List.of(), List.of());

    @BeforeEach
    void setUp() {
        // 真实 TransactionTemplate + mock 事务管理器：同步回调同步执行，upsert 逻辑照常跑；
        // lenient：拉取失败用例不会走到事务模板执行，该 stub 属非必要
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        lenient().when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new ModelCatalogSyncService(client, modelRepository, logRepository, txManager);
    }

    @Test
    @DisplayName("全量同步：新增+更新+跳过统计正确")
    void sync_mixed_addsUpdatesSkips() {
        // 已有模型：gpt-4o（externalId 匹配，将更新）
        Model existingGpt = new Model();
        existingGpt.setId(1L);
        existingGpt.setModelName("gpt-4o");
        existingGpt.setExternalId("openai/gpt-4o");
        existingGpt.setLockedFields(List.of("displayName"));

        when(client.fetch()).thenReturn(List.of(gpt4o, deepseek));
        when(modelRepository.findByExternalId("openai/gpt-4o")).thenReturn(Optional.of(existingGpt));
        when(modelRepository.findByExternalId("deepseek/deepseek-v4-flash")).thenReturn(Optional.empty());
        when(modelRepository.findByModelName("deepseek-v4-flash")).thenReturn(Optional.empty());
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any(CatalogSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncReport report = service.sync();

        assertThat(report.isSuccess()).isTrue();
        assertThat(report.getAddedCount()).isEqualTo(1);
        assertThat(report.getUpdatedCount()).isEqualTo(1);
        assertThat(report.getSkippedCount()).isZero();
        assertThat(report.getFailedCount()).isZero();
        // 新增模型写入 externalId
        ArgumentCaptor<Model> savedCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository, times(2)).save(savedCaptor.capture());
        Model added = savedCaptor.getAllValues().stream()
                .filter(m -> m.getExternalId().equals("deepseek/deepseek-v4-flash")).findFirst().orElseThrow();
        assertThat(added.getSource()).isEqualTo("MODELS_DEV");
        assertThat(added.getModelName()).isEqualTo("deepseek-v4-flash");
        // 成功路径写入 SUCCESS 日志，计数与报告一致
        ArgumentCaptor<CatalogSyncLog> logCaptor = ArgumentCaptor.forClass(CatalogSyncLog.class);
        verify(logRepository).save(logCaptor.capture());
        CatalogSyncLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getResult()).isEqualTo("SUCCESS");
        assertThat(savedLog.getAddedCount()).isEqualTo(1);
        assertThat(savedLog.getUpdatedCount()).isEqualTo(1);
        assertThat(savedLog.getSkippedCount()).isZero();
        assertThat(savedLog.getFailedCount()).isZero();
    }

    @Test
    @DisplayName("modelName 冲突：跳过并计入报告")
    void sync_modelNameConflict_skips() {
        // 已有一个 external_id 不同但 modelName 相同的模型
        Model conflict = new Model();
        conflict.setId(2L);
        conflict.setModelName("gpt-4o");
        conflict.setExternalId("other/gpt-4o");

        when(client.fetch()).thenReturn(List.of(gpt4o));
        when(modelRepository.findByExternalId("openai/gpt-4o")).thenReturn(Optional.empty());
        when(modelRepository.findByModelName("gpt-4o")).thenReturn(Optional.of(conflict));
        when(logRepository.save(any(CatalogSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncReport report = service.sync();

        assertThat(report.getSkippedCount()).isEqualTo(1);
        assertThat(report.getAddedCount()).isZero();
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    @DisplayName("拉取失败：记录 FAILURE 日志并抛异常")
    void sync_fetchFailure_logsFailureAndThrows() {
        when(client.fetch()).thenThrow(new CatalogSyncException("网络错误", null));
        when(logRepository.save(any(CatalogSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.sync()).isInstanceOf(CatalogSyncException.class);

        ArgumentCaptor<CatalogSyncLog> captor = ArgumentCaptor.forClass(CatalogSyncLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("FAILURE");
        // 拉取失败：日志中无已处理计数
        assertThat(captor.getValue().getAddedCount()).isZero();
        assertThat(captor.getValue().getUpdatedCount()).isZero();
        assertThat(captor.getValue().getSkippedCount()).isZero();
        assertThat(captor.getValue().getFailedCount()).isZero();
    }

    @Test
    @DisplayName("存量模型无 externalId：按 modelName 匹配并补写 externalId")
    void sync_existingModelWithoutExternalId_backfillsExternalId() {
        Model legacy = new Model();
        legacy.setId(3L);
        legacy.setModelName("gpt-4o");
        legacy.setSource("BUILTIN");

        when(client.fetch()).thenReturn(List.of(gpt4o));
        when(modelRepository.findByExternalId("openai/gpt-4o")).thenReturn(Optional.empty());
        when(modelRepository.findByModelName("gpt-4o")).thenReturn(Optional.of(legacy));
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any(CatalogSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncReport report = service.sync();

        assertThat(report.getUpdatedCount()).isEqualTo(1);
        assertThat(legacy.getExternalId()).isEqualTo("openai/gpt-4o");
        assertThat(legacy.getSource()).isEqualTo("BUILTIN");  // 不改变来源标记
    }
}
