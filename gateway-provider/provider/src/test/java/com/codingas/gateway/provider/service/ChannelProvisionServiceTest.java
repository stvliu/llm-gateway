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

import com.codingas.gateway.provider.catalog.BatchProvisionRequest;
import com.codingas.gateway.provider.catalog.BatchProvisionResult;
import com.codingas.gateway.provider.catalog.CatalogException;
import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogGateway;
import com.codingas.gateway.provider.catalog.PlanModelCatalogGateway;
import com.codingas.gateway.provider.catalog.ProvisionRequest;
import com.codingas.gateway.provider.catalog.ProvisionResult;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.channel.ChannelEndpointGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceGateway;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderGateway;
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
    private PlanCatalogGateway planCatalogGateway;
    @Mock
    private PlanModelCatalogGateway planModelCatalogGateway;
    @Mock
    private ProviderGateway providerGateway;
    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ChannelEndpointGateway channelEndpointGateway;
    @Mock
    private ModelInstanceGateway modelInstanceGateway;
    @Mock
    private ChannelCredentialGateway channelCredentialGateway;
    @Mock
    private ModelGateway modelGateway;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChannelProvisionService service;

    private static final String PLAN_CODE = "openai-paid";
    private static final String PROVIDER_CODE = "openai";

    @BeforeEach
    void setUp() {
        service = new ChannelProvisionService(
                planCatalogGateway, planModelCatalogGateway, providerGateway,
                channelGateway, channelEndpointGateway, modelInstanceGateway,
                channelCredentialGateway, modelGateway, objectMapper);
    }

    // ==================== provisionFromPlan 测试 ====================

    @Nested
    @DisplayName("provisionFromPlan 开通主流程")
    class ProvisionFromPlanTests {

        @Test
        @DisplayName("套餐不存在时抛 CATALOG_NOT_FOUND")
        void planNotFound_throws() {
            when(planCatalogGateway.findByPlanCode("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.provisionFromPlan("nope"))
                    .isInstanceOf(CatalogException.class)
                    .satisfies(ex -> assertThat(((CatalogException) ex).getCode())
                            .isEqualTo("CATALOG_NOT_FOUND"));
        }

        @Test
        @DisplayName("渠道已开通时返回 SKIPPED")
        void channelExists_returnsSkipped() {
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(stubPlan()));
            Provider existing = new Provider();
            existing.setId(7L);
            existing.setCode(PROVIDER_CODE);
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existing));
            when(channelGateway.existsByProviderIdAndName(7L, PLAN_CODE)).thenReturn(true);

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE);

            assertThat(result.getStatus()).isEqualTo("SKIPPED");
            verify(channelGateway, never()).save(any(Channel.class));
        }

        @Test
        @DisplayName("单参方法：完整开通（端点+定价+上游模型名映射+API Key 批量创建）")
        void singleArg_fullProvision() {
            PlanCatalog catalog = stubPlan();
            catalog.setEndpoints("[{\"protocol\":\"ANTHROPIC\",\"url\":\"https://api.anthropic.com\"}]");
            catalog.setPricing("[{\"providerModelId\":\"claude-sonnet-4-6\",\"inputPrice\":3,"
                    + "\"outputPrice\":15}]");
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();
            // Model 已存在 → 直接复用，不新建
            Model existing = new Model();
            existing.setId(50L);
            existing.setModelName("claude-sonnet-4-6");
            when(modelGateway.findByModelName("claude-sonnet-4-6")).thenReturn(Optional.of(existing));
            when(modelInstanceGateway.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(900L);
                return mi;
            });

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getChannelId()).isEqualTo(1000L);
            assertThat(result.getEndpointCount()).isEqualTo(1);
            assertThat(result.getInstanceCount()).isEqualTo(1);
            verify(modelGateway, never()).save(any(Model.class));
            // 断言上游模型名映射：openai 不在规则表 → null
            ArgumentCaptor<ModelInstance> miCaptor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceGateway).save(miCaptor.capture());
            assertThat(miCaptor.getValue().getUpstreamModelName()).isNull();
        }

        @Test
        @DisplayName("aws-bedrock 供应商应用上游模型名映射规则")
        void awsBedrock_upstreamNameMapping() {
            PlanCatalog catalog = stubPlan();
            catalog.setProviderCode("aws-bedrock");
            catalog.setPricing("[{\"providerModelId\":\"claude-sonnet-4-6\",\"inputPrice\":3}]");
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            // 注意：该用例 providerCode 为 aws-bedrock，需单独 stub
            when(providerGateway.findByCode("aws-bedrock")).thenReturn(Optional.empty());
            stubProviderSaveWithId();
            stubChannelSave();
            when(modelGateway.findByModelName("claude-sonnet-4-6")).thenReturn(Optional.empty());
            when(modelGateway.save(any(Model.class))).thenAnswer(inv -> {
                Model m = inv.getArgument(0);
                m.setId(51L);
                return m;
            });
            when(modelInstanceGateway.save(any(ModelInstance.class))).thenAnswer(inv -> {
                ModelInstance mi = inv.getArgument(0);
                mi.setId(901L);
                return mi;
            });

            service.provisionFromPlan(PLAN_CODE);

            ArgumentCaptor<ModelInstance> miCaptor = ArgumentCaptor.forClass(ModelInstance.class);
            verify(modelInstanceGateway).save(miCaptor.capture());
            assertThat(miCaptor.getValue().getUpstreamModelName())
                    .isEqualTo("anthropic.claude-sonnet-4-6");
        }

        @Test
        @DisplayName("API Key 批量创建：空串跳过、前缀截断、优先级递增")
        void apiKeysBatchCreation() {
            PlanCatalog catalog = stubPlan();
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
            stubProviderCreate();
            stubChannelSave();

            ProvisionRequest request = new ProvisionRequest();
            request.setApiKeys(List.of("sk-long-key-12345678", "   ", "sk-2"));

            ProvisionResult result = service.provisionFromPlan(PLAN_CODE, request);

            assertThat(result.getStatus()).isEqualTo("CREATED");
            ArgumentCaptor<ChannelCredential> captor = ArgumentCaptor.forClass(ChannelCredential.class);
            verify(channelCredentialGateway, times(2)).save(captor.capture());
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
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
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
            when(planCatalogGateway.findByPlanCode(PLAN_CODE)).thenReturn(Optional.of(catalog));
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
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            when(planCatalogGateway.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            // SKIPPED 路径不会走到 save，无需 stubChannelSave
            when(channelGateway.existsByProviderIdAndName(7L, "p-1")).thenReturn(true);

            BatchProvisionRequest request = new BatchProvisionRequest();
            request.setPlanCodes(List.of("p-1"));

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
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            c1.setPlanCode("p-1");
            PlanCatalog c2 = stubPlan();
            c2.setPlanCode("p-2");
            when(planCatalogGateway.findByProviderCode(PROVIDER_CODE)).thenReturn(List.of(c1, c2));
            when(planCatalogGateway.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            when(planCatalogGateway.findByPlanCode("p-2")).thenReturn(Optional.of(c2));
            stubChannelSave();

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, null);

            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getSuccessCount()).isEqualTo(2);
            verify(channelGateway, times(2)).save(any(Channel.class));
        }

        @Test
        @DisplayName("套餐目录缺失时计入 FAILED（CatalogException 路径）")
        void catalogException_countsFailed() {
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            BatchProvisionRequest request = new BatchProvisionRequest();
            request.setPlanCodes(List.of("p-missing"));
            when(planCatalogGateway.findByPlanCode("p-missing")).thenReturn(Optional.empty());

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, request);

            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getResults().get(0).getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("运行时异常计入 FAILED（兜底路径）")
        void runtimeException_countsFailed() {
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.of(existingProvider()));
            PlanCatalog c1 = stubPlan();
            when(planCatalogGateway.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            when(channelGateway.existsByProviderIdAndName(7L, "p-1")).thenThrow(new IllegalStateException("db down"));

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE,
                    reqWithPlanCodes("p-1"));

            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getResults().get(0).getErrorMessage()).contains("db down");
        }

        @Test
        @DisplayName("Provider 不存在时自动级联创建")
        void providerNotExists_autoCreated() {
            when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
            stubProviderSaveWithId();
            BatchProvisionRequest request = new BatchProvisionRequest();
            request.setPlanCodes(List.of());

            BatchProvisionResult result = service.provisionBatch(PROVIDER_CODE, request);

            assertThat(result.getTotalCount()).isZero();
            verify(providerGateway).save(any(Provider.class));
        }
    }

    // ==================== provisionModel 测试 ====================

    @Nested
    @DisplayName("provisionModel 开通模型")
    class ProvisionModelTests {

        @Test
        @DisplayName("模型已存在时返回 SKIPPED")
        void modelExists_returnsSkipped() {
            when(modelGateway.findByModelName("gpt-4")).thenReturn(Optional.of(new Model()));

            ProvisionResult result = service.provisionModel("gpt-4");

            assertThat(result.getStatus()).isEqualTo("SKIPPED");
            verify(modelGateway, never()).save(any(Model.class));
        }

        @Test
        @DisplayName("模型不存在时创建并返回 CREATED")
        void modelNotExists_creates() {
            when(modelGateway.findByModelName("gpt-4")).thenReturn(Optional.empty());
            when(modelGateway.save(any(Model.class))).thenAnswer(inv -> {
                Model m = inv.getArgument(0);
                m.setId(60L);
                return m;
            });

            ProvisionResult result = service.provisionModel("gpt-4");

            assertThat(result.getStatus()).isEqualTo("CREATED");
            assertThat(result.getChannelId()).isEqualTo(60L);
            ArgumentCaptor<Model> captor = ArgumentCaptor.forClass(Model.class);
            verify(modelGateway).save(captor.capture());
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
        when(providerGateway.findByCode(PROVIDER_CODE)).thenReturn(Optional.empty());
        stubProviderSaveWithId();
    }

    private void stubProviderSaveWithId() {
        when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
            Provider p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
    }

    /** channel 保存返回带 ID 的 Channel，exists 默认 false */
    private void stubChannelSave() {
        lenient().when(channelGateway.existsByProviderIdAndName(any(), anyString())).thenReturn(false);
        when(channelGateway.save(any(Channel.class))).thenAnswer(inv -> {
            Channel c = inv.getArgument(0);
            c.setId(1000L);
            return c;
        });
    }

    private BatchProvisionRequest reqWithPlanCodes(String... codes) {
        BatchProvisionRequest req = new BatchProvisionRequest();
        req.setPlanCodes(List.of(codes));
        return req;
    }
}
