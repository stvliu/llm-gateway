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
package com.codingas.gateway.provider.service;

import com.codingas.gateway.provider.BuiltinDataLoader;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.catalog.PlanModelCatalog;
import com.codingas.gateway.provider.catalog.PlanModelCatalogRepository;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BuiltinDataLoader 单元测试
 *
 * <p>覆盖启动装载逻辑：空表全量加载（ADDED）、幂等跳过、forceReload 强制加载、
 * 已存在记录更新（UPDATED + 字段拷贝）、异常兜底（不抛出）、capabilities/modalities 解析分支。</p>
 *
 * <p>测试资源 fixture（model-specs.json/plan-models.json/plans.json/providers.json）
 * 是刻意裁剪的测试专用子集（providers 2 / model-specs 4 / plans 2 / plan-models 2，
 * 含测试专用条目如 malformed-model、claude-sonnet-4），并非 gateway-boot 真实
 * catalog 的复制——断言计数依赖本 fixture 内容，增删 fixture 条目须同步更新
 * 装载计数断言。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuiltinDataLoader 单元测试")
class BuiltinDataLoaderTest {

    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private PlanCatalogRepository planCatalogRepository;
    @Mock
    private PlanModelCatalogRepository planModelCatalogRepository;
    @Mock
    private ModelRepository modelRepository;

    private BuiltinDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new BuiltinDataLoader(
                providerRepository, planCatalogRepository, planModelCatalogRepository, modelRepository,
                new ObjectMapper());
    }

    /** 所有查找均返回空、save 返回入参，进入纯 ADDED 装载（findAll 由调用 loadIfNeeded 的用例单独 stub） */
    private void stubEmptyTables() {
        when(providerRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelRepository.findByModelName(anyString())).thenReturn(Optional.empty());
        when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planCatalogRepository.findByPlanCode(anyString())).thenReturn(Optional.empty());
        when(planCatalogRepository.save(any(PlanCatalog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planModelCatalogRepository.findByPlanCodeAndModelName(anyString(), anyString())).thenReturn(Optional.empty());
        when(planModelCatalogRepository.save(any(PlanModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("表为空时加载四类内置数据，字段映射正确")
    void 空表加载四类数据() {
        stubEmptyTables();
        when(providerRepository.findAll()).thenReturn(List.of());

        loader.loadIfNeeded();

        // 供应商：2 条，openai 字段映射正确，priority=100
        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        verify(providerRepository, times(2)).save(providerCaptor.capture());
        Provider openai = providerCaptor.getAllValues().stream()
                .filter(p -> "openai".equals(p.getCode()))
                .findFirst().orElseThrow();
        assertThat(openai.getName()).isEqualTo("OpenAI");
        assertThat(openai.getPriority()).isEqualTo(100);
        assertThat(openai.getLogoUrl()).isEqualTo("https://cdn.example.com/icons/openai.png");

        // 模型：4 条，gpt-4.1 的 capabilities/modalities 解析为 Map/List
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository, times(4)).save(modelCaptor.capture());
        Model gpt = modelCaptor.getAllValues().stream()
                .filter(m -> "gpt-4.1".equals(m.getModelName()))
                .findFirst().orElseThrow();
        assertThat(gpt.getModelFamily()).isEqualTo("gpt-4.1");
        assertThat(gpt.getCapabilities()).containsEntry("tool_use", true);
        assertThat(gpt.getModalities()).containsExactly("text");

        // 套餐：2 条，billingMode 正确；openai_standard 序列化 endpoints/pricing
        ArgumentCaptor<PlanCatalog> planCaptor = ArgumentCaptor.forClass(PlanCatalog.class);
        verify(planCatalogRepository, times(2)).save(planCaptor.capture());
        PlanCatalog plan = planCaptor.getAllValues().stream()
                .filter(p -> "openai_standard".equals(p.getPlanCode()))
                .findFirst().orElseThrow();
        assertThat(plan.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(plan.getEndpoints()).contains("api.openai.com");

        // 套餐模型关联：2 条
        verify(planModelCatalogRepository, times(2)).save(any(PlanModelCatalog.class));
    }

    @Test
    @DisplayName("已有数据时 loadIfNeeded 跳过装载（幂等）")
    void 已有数据跳过装载() {
        when(providerRepository.findAll()).thenReturn(List.of(new Provider()));

        loader.loadIfNeeded();

        verify(providerRepository, never()).save(any(Provider.class));
        verify(modelRepository, never()).save(any(Model.class));
        verify(planCatalogRepository, never()).save(any(PlanCatalog.class));
        verify(planModelCatalogRepository, never()).save(any(PlanModelCatalog.class));
    }

    @Test
    @DisplayName("run 委托给 loadIfNeeded（有数据时同样跳过）")
    void run_委托给_loadIfNeeded() {
        when(providerRepository.findAll()).thenReturn(List.of(new Provider()));

        loader.run();

        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("forceReload 不检查表是否为空，直接加载")
    void forceReload_强制加载() {
        stubEmptyTables();

        loader.forceReload();

        verify(providerRepository, times(2)).save(any(Provider.class));
        verify(modelRepository, times(4)).save(any(Model.class));
    }

    @Test
    @DisplayName("已存在记录走 UPDATED 分支并拷贝业务字段")
    void 已存在记录更新分支() {
        stubEmptyTables();
        // 第一条记录已存在 → 走 UPDATED + 字段拷贝
        Provider existingProvider = new Provider();
        existingProvider.setCode("openai");
        when(providerRepository.findByCode("openai")).thenReturn(Optional.of(existingProvider));

        Model existingModel = new Model();
        existingModel.setModelName("gpt-4.1");
        when(modelRepository.findByModelName("gpt-4.1")).thenReturn(Optional.of(existingModel));

        PlanCatalog existingPlan = new PlanCatalog();
        existingPlan.setPlanCode("openai_standard");
        when(planCatalogRepository.findByPlanCode("openai_standard")).thenReturn(Optional.of(existingPlan));

        PlanModelCatalog existingPlanModel = new PlanModelCatalog();
        existingPlanModel.setPlanCode("openai_standard");
        existingPlanModel.setModelName("gpt-4.1");
        when(planModelCatalogRepository.findByPlanCodeAndModelName("openai_standard", "gpt-4.1"))
                .thenReturn(Optional.of(existingPlanModel));

        loader.forceReload();

        // UPDATED：拷贝源字段到已存在实体并保存
        verify(providerRepository).save(existingProvider);
        assertThat(existingProvider.getName()).isEqualTo("OpenAI");
        assertThat(existingProvider.getPriority()).isEqualTo(100);

        verify(modelRepository).save(existingModel);
        assertThat(existingModel.getDisplayName()).isEqualTo("GPT-4.1");
        assertThat(existingModel.getContextWindow()).isEqualTo(1047576);

        verify(planCatalogRepository).save(existingPlan);
        assertThat(existingPlan.getPlanName()).isEqualTo("按量付费");
        assertThat(existingPlan.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);

        verify(planModelCatalogRepository).save(existingPlanModel);

        // 第二条记录仍走 ADDED
        verify(providerRepository, times(2)).save(any(Provider.class));
        verify(modelRepository, times(4)).save(any(Model.class));
    }

    @Test
    @DisplayName("endpoints/pricing 为 null 时置空，不抛异常")
    void 空endpoints与pricing() {
        stubEmptyTables();

        loader.forceReload();

        ArgumentCaptor<PlanCatalog> planCaptor = ArgumentCaptor.forClass(PlanCatalog.class);
        verify(planCatalogRepository, times(2)).save(planCaptor.capture());
        PlanCatalog anthropic = planCaptor.getAllValues().stream()
                .filter(p -> "anthropic_standard".equals(p.getPlanCode()))
                .findFirst().orElseThrow();
        assertThat(anthropic.getEndpoints()).isNull();
        assertThat(anthropic.getPricing()).isNull();
    }

    @Test
    @DisplayName("capabilities/modalities 为 null 或非法时兜底为空集合")
    void capabilities与modalities解析兜底() {
        stubEmptyTables();

        loader.forceReload();

        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelRepository, times(4)).save(modelCaptor.capture());
        // capabilities=null → 空 Map
        Model claude = modelCaptor.getAllValues().stream()
                .filter(m -> "claude-sonnet-4".equals(m.getModelName()))
                .findFirst().orElseThrow();
        assertThat(claude.getCapabilities()).isEmpty();
        assertThat(claude.getModalities()).isEmpty();
        // 非法值 → 解析失败兜底为空
        Model malformed = modelCaptor.getAllValues().stream()
                .filter(m -> "malformed-model".equals(m.getModelName()))
                .findFirst().orElseThrow();
        assertThat(malformed.getCapabilities()).isEmpty();
        assertThat(malformed.getModalities()).isEmpty();
    }

    @Test
    @DisplayName("装载异常被吞掉，应用可正常启动")
    void 装载异常不抛出() {
        when(providerRepository.findAll()).thenReturn(List.of());
        when(providerRepository.save(any(Provider.class))).thenThrow(new RuntimeException("DB down"));

        // doLoad 捕获异常仅记日志，不冒出
        assertThatCode(() -> loader.loadIfNeeded()).doesNotThrowAnyException();
    }
}
