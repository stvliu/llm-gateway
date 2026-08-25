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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.catalog.BatchProvisionResult;
import com.codingas.gateway.provider.catalog.CatalogException;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.catalog.PlanModelCatalogRepository;
import com.codingas.gateway.provider.catalog.ProvisionResult;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChannelProvisionService 单元测试（核心开通/级联/批量逻辑）
 *
 * <p>inlineProvider 三路径已由 {@code ChannelProvisionServiceInlineProviderTest} 覆盖，此处
 * 补充开通主流程、批量开通、模型级联、上游模型名映射、JSON 解析容错等分支。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelProvisionService 开通逻辑测试")
class ChannelProvisionServiceTest {

    @Mock
    private PlanCatalogRepository planCatalogRepository;
    @Mock
    private PlanModelCatalogRepository planModelCatalogRepository;
    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelEndpointRepository channelEndpointRepository;
    @Mock
    private ModelInstanceRepository modelInstanceRepository;
    @Mock
    private ChannelCredentialRepository channelCredentialRepository;
    @Mock
    private ModelRepository modelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChannelProvisionService service;

    private static final String PLAN_CODE = "openai-paid";
    private static final String PROVIDER_CODE = "openai";

    @BeforeEach
    void setUp() {
        service = new ChannelProvisionService(
                planCatalogRepository, planModelCatalogRepository, providerRepository,
                channelRepository, channelEndpointRepository, modelInstanceRepository,
                channelCredentialRepository, modelRepository, objectMapper);
    }

    // ==================== provisionFromPlan 测试 ====================

    @Nested
    @DisplayName("provisionFromPlan 开通主流程")
    class ProvisionFromPlanTests {

        @Test
        @DisplayName("套餐不存在时抛 CATALOG_NOT_FOUND")
        void planNotFound_throws() {
            when(planCatalogRepository.findByPlanCode("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.provisionFromPlan("nope"))
                    .isInstanceOf(CatalogException.class)
                    .satisfies(ex -> assertThat(((CatalogException) ex).getCode())
                            .isEqualTo("CATALOG_NOT_FOUND"));
        }

        @Test
        @DisplayName("渠道已开通时返回 SKIPPED")
        void channelExists_returnsSkipped() {
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
            Provider existing = new Provider();
            existing.setId(7L);
            existing.setCode(PROVIDER_CODE);
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existing));
            when(channelRepository.existsByProviderIdAndName(7L, PLAN_CODE)).thenReturn(true);

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE);

            assertThat(result.getStatus()).isEqualTo("SKIPPED");
            verify(channelRepository, never()).save(any(Channel.class));
        }

        @Test
        @DisplayName("单参方法：完整开通（端点+定价+上游模型名映射+API Key 批量创建）")
        void singleArg_fullProvision() {
            PlanCatalog catalog = stubPlan();
            catalog.setEndpoints("[{\"protocol\":\"ANTHROPIC\",\"url\":\"https://api.anthropic.com\"}]");
            catalog.setPricing("[{\"providerModelId\":\"claude-sonnet-4-6\",\"inputPrice\":3,"
                    + "\"outputPrice\":15}]");
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();
            // Model 已存在 → 直接复用，不新建
            Model existing = new Model();
            existing.setId(50L);
            existing.setModelName("claude-sonnet-4-6");
            when(modelRepository.findByModelName("claude-sonnet-4-6")).thenReturn(Optional.of(existing));
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(900L);
                return mi;
            });

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getChannelId()).isEqualTo(1000L);
            assertThat(result.getEndpointCount()).isEqualTo(1);
            assertThat(result.getInstanceCount()).isEqualTo(1);
            verify(modelRepository, never()).save(any(Model.class));
            // 断言上游模型名映射：openai 不在规则表 → null
            ArgumentCaptor<ModelInstance> miCaptor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceRepository).save(miCaptor.capture());
            assertThat(miCaptor.getValue().getUpstreamModelName()).isNull();
        }

        @Test
        @DisplayName("aws-bedrock 供应商应用上游模型名映射规则")
        void awsBedrock_upstreamNameMapping() {
            PlanCatalog catalog = stubPlan();
            catalog.setProviderCode("aws-bedrock");
            catalog.setPricing("[{\"providerModelId\":\"claude-sonnet-4-6\",\"inputPrice\":3}]");
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            // 注意：该用例 providerCode 为 aws-bedrock，需单独 stub
            when(providerRepository.findByCode("aws-bedrock")).thenReturn(Optional.empty());
            stubProviderSaveWithId();
            stubChannelSave();
            when(modelRepository.findByModelName("claude-sonnet-4-6")).thenReturn(Optional.empty());
            when(modelRepository.save(any(Model.class))).thenAnswer(inv -> {
                Model m = inv.getArgument(0);
                m.setId(51L);
                return m;
            });
            when(modelInstanceRepository.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(901L);
                return mi;
            });

            service.provisionFromPlan(PLAN_CODE);

            ArgumentCaptor<ModelInstance> miCaptor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceRepository).save(miCaptor.capture());
            assertThat(miCaptor.getValue().getUpstreamModelName())
                    .isEqualTo("anthropic.claude-sonnet-4-6");
        }

        @Test
        @DisplayName("API Key 批量创建：空串跳过、前缀截断、优先级递增")
        void apiKeysBatchCreation() {
            PlanCatalog catalog = stubPlan();
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();

            ProvisionCommand request =
                    new ProvisionCommand(List.of("sk-long-key-12345678", "   ", "sk-2"), null);

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE, request);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            ArgumentCaptor<ChannelCredential> captor = ArgumentCaptor.forClass(ChannelCredential.class);
            verify(channelCredentialRepository, times(2)).save(captor.capture());
            List<ChannelCredential> saved = captor.getAllValues();
            assertThat(saved.get(0).getApiKeyPrefix()).isEqualTo("sk-long-");
            assertThat(saved.get(0).getPriority()).isEqualTo(1);
            assertThat(saved.get(1).getApiKeyPrefix()).isEqualTo("sk-2");
            assertThat(saved.get(1).getPriority()).isEqualTo(2);
            assertThat(saved.get(0).getWeight()).isEqualTo(100);
        }

        @Test
        @DisplayName("pricing 模型名为空时抛 INVALID_CATALOG_DATA")
        void blankModelName_throws() {
            PlanCatalog catalog = stubPlan();
            catalog.setPricing("[{\"providerModelId\":\"  \"}]");
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();

            assertThatThrownBy(() -> service.provisionFromPlan(PLAN_CODE))
                    .isInstanceOf(CatalogException.class)
                    .satisfies(ex -> assertThat(((CatalogException) ex).getCode())
                            .isEqualTo("INVALID_CATALOG_DATA"));
        }

        @Test
        @DisplayName("endpoints/pricing JSON 解析失败时容错为空列表")
        void parseFailure_tolerated() {
            PlanCatalog catalog = stubPlan();
            catalog.setEndpoints("[{bad json}");
            catalog.setPricing("[{bad json}");
            when(planCatalogRepository.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getEndpointCount()).isZero();
            assertThat(result.getInstanceCount()).isZero();
        }
    }

    // ==================== provisionBatch 测试 ====================

    @Nested
    @DisplayName("provisionBatch 批量开通")
    class ProvisionBatchTests {

        @Test
        @DisplayName("指定 planCodes 时仅开通指定套餐")
        void withPlanCodes() {
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            // SKIPPED 路径不会走到 save，无需 stubChannelSave
            when(channelRepository.existsByProviderIdAndName(7L, "p-1")).thenReturn(true);

            BatchProvisionCommand request = new BatchProvisionCommand(List.of("p-1"));

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, request);

            assertThat(result.getTotalCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getSuccessCount()).isZero();
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("SKIPPED");
            // findByProviderCode 始终被调用（allPlans 预取），但指定 planCodes 时不消费其结果
        }

        @Test
        @DisplayName("未指定 planCodes 时开通供应商下全部套餐")
        void withoutPlanCodes_usesAllPlans() {
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            c1.setPlanCode("p-1");
            PlanCatalog c2 = stubPlan();
            c2.setPlanCode("p-2");
            when(planCatalogRepository.findByProviderCode(PROVIDER_CODE)).thenReturn(List.of(c1, c2));
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            when(planCatalogRepository.findByPlanCode("p-2")).thenReturn(Optional.of(c2));
            stubChannelSave();

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, null);

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getSuccessCount()).isEqualTo(2);
            verify(channelRepository, times(2)).save(any(Channel.class));
        }

        @Test
        @DisplayName("套餐目录缺失时计入 FAILED（CatalogException 路径）")
        void catalogException_countsFailed() {
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            BatchProvisionCommand request = new BatchProvisionCommand(List.of("p-missing"));
            when(planCatalogRepository.findByPlanCode("p-missing")).thenReturn(Optional.empty());

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, request);

            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("运行时异常计入 FAILED（兜底路径）")
        void runtimeException_countsFailed() {
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            when(channelRepository.existsByProviderIdAndName(7L, "p-1")).thenThrow(new IllegalStateException("db down"));

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE,
                    reqWithPlanCodes("p-1"));

            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getResults().get(0).getErrorMessage()).contains("db down");
        }

        @Test
        @DisplayName("Provider 不存在时自动级联创建")
        void providerNotExists_autoCreated() {
            when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
            stubProviderSaveWithId();
            BatchProvisionCommand request = new BatchProvisionCommand(List.of());

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, request);

            assertThat(result.getTotalCount()).isZero();
            verify(providerRepository).save(any(Provider.class));
        }
    }

    // ==================== provisionModel 测试 ====================

    @Nested
    @DisplayName("provisionModel 开通模型")
    class ProvisionModelTests {

        @Test
        @DisplayName("模型已存在时返回 SKIPPED")
        void modelExists_returnsSkipped() {
            when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(new Model()));

            ProvisionResult result = service.provisionModel("gpt-4");

            assertThat(result.getStatus()).isEqualTo("SKIPPED");
            verify(modelRepository, never()).save(any(Model.class));
        }

        @Test
        @DisplayName("模型不存在时创建并返回 CREATED")
        void modelNotExists_creates() {
            when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.empty());
            when(modelRepository.save(any(Model.class))).thenAnswer(inv -> {
                Model m = inv.getArgument(0);
                m.setId(60L);
                return m;
            });

            ProvisionResult result = service.provisionModel("gpt-4");

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getChannelId()).isEqualTo(60L);
            ArgumentCaptor<Model> captor = ArgumentCaptor.forClass(Model.class);
            verify(modelRepository).save(captor.capture());
            assertThat(captor.getValue().getModelName()).isEqualTo("gpt-4");
        }
    }

    // ==================== 辅助方法 ====================

    private PlanCatalog stubPlan() {
        PlanCatalog plan = new PlanCatalog();
        plan.setPlanCode(PLAN_CODE);
        plan.setProviderCode(PROVIDER_CODE);
        plan.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        plan.setEndpoints(null);
        plan.setPricing(null);
        return plan;
    }

    private Provider existingProvider() {
        Provider p = new Provider();
        p.setId(7L);
        p.setCode(PROVIDER_CODE);
        return p;
    }

    /** provider 不存在 → save 返回带 ID 的 Provider（新建路径） */
    private void stubProviderCreate() {
        when(providerRepository.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSaveWithId();
    }

    private void stubProviderSaveWithId() {
        when(providerRepository.save(any(Provider.class))).thenAnswer(inv -> {
            Provider p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    /** channel 保存返回带 ID 的 Channel，exists 默认 false */
    private void stubChannelSave() {
        lenient().when(channelRepository.existsByProviderIdAndName(any(), anyString())).thenReturn(false);
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
            Channel c = inv.getArgument(0);
            c.setId(1000L);
            return c;
        });
    }

    private BatchProvisionCommand reqWithPlanCodes(String... codes) {
        return new BatchProvisionCommand(List.of(codes));
    }
}
