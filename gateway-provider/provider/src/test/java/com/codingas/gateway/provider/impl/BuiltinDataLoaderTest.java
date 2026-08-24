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
package com.codingas.gateway.provider.impl;

import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogGateway;
import com.codingas.gateway.provider.catalog.PlanModelCatalog;
import com.codingas.gateway.provider.catalog.PlanModelCatalogGateway;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderGateway;
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
 * <p>测试资源 fixture 位于 src/test/resources/catalog/ 下（真实 catalog 在 gateway-boot 模块）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuiltinDataLoader 单元测试")
class BuiltinDataLoaderTest {

    @Mock
    private ProviderGateway providerGateway;
    @Mock
    private PlanCatalogGateway planCatalogGateway;
    @Mock
    private PlanModelCatalogGateway planModelCatalogGateway;
    @Mock
    private ModelGateway modelGateway;

    private BuiltinDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new BuiltinDataLoader(
                providerGateway, planCatalogGateway, planModelCatalogGateway, modelGateway,
                new ObjectMapper());
    }

    /** 所有查找均返回空、save 返回入参，进入纯 ADDED 装载（findAll 由调用 loadIfNeeded 的用例单独 stub） */
    private void stubEmptyTables() {
        when(providerGateway.findByCode(anyString())).thenReturn(Optional.empty());
        when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelGateway.findByModelName(anyString())).thenReturn(Optional.empty());
        when(modelGateway.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planCatalogGateway.findByPlanCode(anyString())).thenReturn(Optional.empty());
        when(planCatalogGateway.save(any(PlanCatalog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planModelCatalogGateway.findByPlanCodeAndModelName(anyString(), anyString())).thenReturn(Optional.empty());
        when(planModelCatalogGateway.save(any(PlanModelCatalog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("表为空时加载四类内置数据，字段映射正确")
    void 空表加载四类数据() {
        stubEmptyTables();
        when(providerGateway.findAll()).thenReturn(List.of());

        loader.loadIfNeeded();

        // 供应商：2 条，openai 字段映射正确，priority=100
        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        verify(providerGateway, times(2)).save(providerCaptor.capture());
        Provider openai = providerCaptor.getAllValues().stream()
                .filter(p -> "openai".equals(p.getCode()))
                .findFirst().orElseThrow();
        assertThat(openai.getName()).isEqualTo("OpenAI");
        assertThat(openai.getPriority()).isEqualTo(100);
        assertThat(openai.getLogoUrl()).isEqualTo("https://cdn.example.com/icons/openai.png");

        // 模型：4 条，gpt-4.1 的 capabilities/modalities 解析为 Map/List
        ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(modelGateway, times(4)).save(modelCaptor.capture());
        Model gpt = modelCaptor.getAllValues().stream()
                .filter(m -> "gpt-4.1".equals(m.getModelName()))
                .findFirst().orElseThrow();
        assertThat(gpt.getModelFamily()).isEqualTo("gpt-4.1");
        assertThat(gpt.getCapabilities()).containsEntry("tool_use", true);
        assertThat(gpt.getModalities()).containsExactly("text");

        // 套餐：2 条，billingMode 正确；openai_standard 序列化 endpoints/pricing
        ArgumentCaptor<PlanCatalog> planCaptor = ArgumentCaptor.forClass(PlanCatalog.class);
        verify(planCatalogGateway, times(2)).save(planCaptor.capture());
        PlanCatalog plan = planCaptor.getAllValues().stream()
                .filter(p -> "openai_standard".equals(p.getPlanCode()))
                .findFirst().orElseThrow();
        assertThat(plan.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(plan.getEndpoints()).contains("api.openai.com");

        // 套餐模型关联：2 条
        verify(planModelCatalogGateway, times(2)).save(any(PlanModelCatalog.class));
    }

    @Test
    @DisplayName("已有数据时 loadIfNeeded 跳过装载（幂等）")
    void 已有数据跳过装载() {
        when(providerGateway.findAll()).thenReturn(List.of(new Provider()));

        loader.loadIfNeeded();

        verify(providerGateway, never()).save(any(Provider.class));
        verify(modelGateway, never()).save(any(Model.class));
        verify(planCatalogGateway, never()).save(any(PlanCatalog.class));
        verify(planModelCatalogGateway, never()).save(any(PlanModelCatalog.class));
    }

    @Test
    @DisplayName("run 委托给 loadIfNeeded（有数据时同样跳过）")
    void run_委托给_loadIfNeeded() {
        when(providerGateway.findAll()).thenReturn(List.of(new Provider()));

        loader.run();

        verify(providerGateway, never()).save(any(Provider.class));
    }

    @Test
    @DisplayName("forceReload 不检查表是否为空，直接加载")
    void forceReload_强制加载() {
        stubEmptyTables();

        loader.forceReload();

        verify(providerGateway, times(2)).save(any(Provider.class));
        verify(modelGateway, times(4)).save(any(Model.class));
    }

    @Test
    @DisplayName("已存在记录走 UPDATED 分支并拷贝业务字段")
    void 已存在记录更新分支() {
        stubEmptyTables();
        // 第一条记录已存在 → 走 UPDATED + 字段拷贝
        Provider existingProvider = new Provider();
        existingProvider.setCode("openai");
        when(providerGateway.findByCode("openai")).thenReturn(Optional.of(existingProvider));

        Model existingModel = new Model();
        existingModel.setModelName("gpt-4.1");
        when(modelGateway.findByModelName("gpt-4.1")).thenReturn(Optional.of(existingModel));

        PlanCatalog existingPlan = new PlanCatalog();
        existingPlan.setPlanCode("openai_standard");
        when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(existingPlan));

        PlanModelCatalog existingPlanModel = new PlanModelCatalog();
        existingPlanModel.setPlanCode("openai_standard");
        existingPlanModel.setModelName("gpt-4.1");
        when(planModelCatalogGateway.findByPlanCodeAndModelName("openai_standard", "gpt-4.1"))
                .thenReturn(Optional.of(existingPlanModel));

        loader.forceReload();

        // UPDATED：拷贝源字段到已存在实体并保存
        verify(providerGateway).save(existingProvider);
        assertThat(existingProvider.getName()).isEqualTo("OpenAI");
        assertThat(existingProvider.getPriority()).isEqualTo(100);

        verify(modelGateway).save(existingModel);
        assertThat(existingModel.getDisplayName()).isEqualTo("GPT-4.1");
        assertThat(existingModel.getContextWindow()).isEqualTo(1047576);

        verify(planCatalogGateway).save(existingPlan);
        assertThat(existingPlan.getPlanName()).isEqualTo("按量付费");
        assertThat(existingPlan.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);

        verify(planModelCatalogGateway).save(existingPlanModel);

        // 第二条记录仍走 ADDED
        verify(providerGateway, times(2)).save(any(Provider.class));
        verify(modelGateway, times(4)).save(any(Model.class));
    }

    @Test
    @DisplayName("endpoints/pricing 为 null 时置空，不抛异常")
    void 空endpoints与pricing() {
        stubEmptyTables();

        loader.forceReload();

        ArgumentCaptor<PlanCatalog> planCaptor = ArgumentCaptor.forClass(PlanCatalog.class);
        verify(planCatalogGateway, times(2)).save(planCaptor.capture());
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
        verify(modelGateway, times(4)).save(modelCaptor.capture());
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
        when(providerGateway.findAll()).thenReturn(List.of());
        when(providerGateway.save(any(Provider.class))).thenThrow(new RuntimeException("DB down"));

        // doLoad 捕获异常仅记日志，不冒出
        assertThatCode(() -> loader.loadIfNeeded()).doesNotThrowAnyException();
    }
}
