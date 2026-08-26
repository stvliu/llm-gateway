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
package com.codingas.gateway.provider.catalog;

import com.codingas.gateway.provider.catalog.CatalogException;
import com.codingas.gateway.provider.catalog.PlanDetailResponse;
import com.codingas.gateway.provider.catalog.PlanModelCatalog;
import com.codingas.gateway.provider.catalog.PlanModelCatalogRepository;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PlanCatalogManagerImpl 单元测试
 *
 * <p>覆盖全部 public 方法的分支：关键字查询、物化状态判断、JSON 解析成功/失败、
 * 模型能力提取、供应商反查等。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanCatalogManagerImpl 单元测试")
class PlanCatalogManagerImplTest {

    @Mock
    private PlanCatalogRepository planCatalogRepository;
    @Mock
    private PlanModelCatalogRepository planModelCatalogRepository;
    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ModelRepository modelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PlanCatalogManagerImpl service;

    @BeforeEach
    void setUp() {
        service = new PlanCatalogManagerImpl(
                planCatalogRepository, planModelCatalogRepository,
                providerRepository, channelRepository, modelRepository,
                objectMapper);
    }

    // ==================== listProviderCatalogs 测试 ====================

    @Nested
    @DisplayName("listProviderCatalogs 供应商目录查询")
    class ListProviderCatalogsTests {

        @Test
        @DisplayName("keyword 为空时查询全部")
        void keywordNull_returnsAll() {
            Provider p1 = provider("openai", "OpenAI");
            Provider p2 = provider("anthropic", "Anthropic");
            when(providerRepository.findAll()).thenReturn(List.of(p1, p2));

            List<ProviderCatalogResponse> result = service.listProviderCatalogs(null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("openai");
            assertThat(result.get(0).getName()).isEqualTo("OpenAI");
            assertThat(result.get(0).getMaterialized()).isTrue();
            verify(providerRepository, never()).findByKeyword(any());
        }

        @Test
        @DisplayName("keyword 非空时按关键字查询")
        void keywordPresent_usesFindByKeyword() {
            Provider p = provider("openai", "OpenAI");
            when(providerRepository.findByKeyword("openai")).thenReturn(List.of(p));

            List<ProviderCatalogResponse> result = service.listProviderCatalogs("openai");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("OpenAI");
            verify(providerRepository, never()).findAll();
        }
    }

    // ==================== listPlanCatalogs 测试 ====================

    @Nested
    @DisplayName("listPlanCatalogs 套餐目录查询")
    class ListPlanCatalogsTests {

        @Test
        @DisplayName("providerCode 为空时查询全部（含 billingMode 空）")
        void providerCodeNull_returnsAll() {
            PlanCatalog c1 = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            PlanCatalog c2 = plan("p-2", "openai", "Plan 2", null);
            when(planCatalogRepository.findAll()).thenReturn(List.of(c1, c2));
            // 物化状态：plan 存在但 provider 不存在 → false
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c1));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());
            when(planCatalogRepository.findByPlanCode("p-2")).thenReturn(Optional.of(c2));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

            List<PlanCatalogResponse> result = service.listPlanCatalogs(null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getBillingMode()).isEqualTo("PAY_AS_YOU_GO");
            assertThat(result.get(0).getMaterialized()).isFalse();
            assertThat(result.get(1).getBillingMode()).isNull();
        }

        @Test
        @DisplayName("providerCode 非空时按供应商查询")
        void providerCodePresent_usesFindByProviderCode() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.SUBSCRIPTION);
            when(planCatalogRepository.findByProviderCode("openai")).thenReturn(List.of(c));

            List<PlanCatalogResponse> result = service.listPlanCatalogs("openai");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlanCode()).isEqualTo("p-1");
            assertThat(result.get(0).getBillingMode()).isEqualTo("SUBSCRIPTION");
            verify(planCatalogRepository, never()).findAll();
        }

        @Test
        @DisplayName("物化状态：provider 存在且渠道已创建 → true")
        void materialized_providerAndChannelExists() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            Provider provider = provider("openai", "OpenAI");
            provider.setId(10L);
            when(planCatalogRepository.findByProviderCode("openai")).thenReturn(List.of(c));
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelRepository.existsByProviderIdAndName(10L, "p-1")).thenReturn(true);

            List<PlanCatalogResponse> result = service.listPlanCatalogs("openai");

            assertThat(result.get(0).getMaterialized()).isTrue();
        }
    }

    // ==================== getPlanDetail 测试 ====================

    @Nested
    @DisplayName("getPlanDetail 套餐详情查询")
    class GetPlanDetailTests {

        @Test
        @DisplayName("套餐不存在时抛 PLAN_NOT_FOUND")
        void planNotFound_throws() {
            when(planCatalogRepository.findByPlanCode("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPlanDetail("nope"))
                    .isInstanceOf(CatalogException.class)
                    .satisfies(ex -> assertThat(((CatalogException) ex).getCode())
                            .isEqualTo("PLAN_NOT_FOUND"));
        }

        @Test
        @DisplayName("endpoints/pricing 为空时返回空列表")
        void emptyEndpointsAndPricing_returnsEmptyLists() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            c.setDescription("desc");
            c.setEndpoints(null);
            c.setPricing(null);
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

            PlanDetailResponse result = service.getPlanDetail("p-1");

            assertThat(result.getPlanCode()).isEqualTo("p-1");
            assertThat(result.getDescription()).isEqualTo("desc");
            assertThat(result.getEndpoints()).isEmpty();
            assertThat(result.getPricing()).isEmpty();
            assertThat(result.getBillingMode()).isEqualTo("PAY_AS_YOU_GO");
            assertThat(result.getMaterialized()).isFalse();
        }

        @Test
        @DisplayName("endpoints/pricing 解析成功")
        void parseSuccess() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            c.setEndpoints("[{\"protocol\":\"openai\",\"url\":\"https://api.openai.com/v1\"}]");
            c.setPricing("[{\"providerModelId\":\"gpt-4\",\"inputPrice\":3.5,\"outputPrice\":7.5,"
                    + "\"cacheReadPrice\":1.5}]");
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

            PlanDetailResponse result = service.getPlanDetail("p-1");

            assertThat(result.getEndpoints()).hasSize(1);
            assertThat(result.getEndpoints().get(0).getProtocol()).isEqualTo("openai");
            assertThat(result.getEndpoints().get(0).getUrl()).isEqualTo("https://api.openai.com/v1");
            assertThat(result.getPricing()).hasSize(1);
            assertThat(result.getPricing().get(0).getProviderModelId()).isEqualTo("gpt-4");
            assertThat(result.getPricing().get(0).getInputPrice()).isEqualByComparingTo("3.5");
            assertThat(result.getPricing().get(0).getOutputPrice()).isEqualByComparingTo("7.5");
            assertThat(result.getPricing().get(0).getCacheReadPrice()).isEqualByComparingTo("1.5");
        }

        @Test
        @DisplayName("billingMode 为空且价格为字符串/缺省时容错解析")
        void nullBillingModeAndStringPrice() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", null);
            // inputPrice 为字符串 → toBigDecimal 走 String 分支；outputPrice 缺省 → null 分支
            c.setPricing("[{\"providerModelId\":\"gpt-4\",\"inputPrice\":\"3.5\"}]");
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

            PlanDetailResponse result = service.getPlanDetail("p-1");

            assertThat(result.getBillingMode()).isNull();
            assertThat(result.getPricing()).hasSize(1);
            assertThat(result.getPricing().get(0).getInputPrice()).isEqualByComparingTo("3.5");
            assertThat(result.getPricing().get(0).getOutputPrice()).isNull();
        }

        @Test
        @DisplayName("endpoints/pricing 解析失败时返回空列表并容错")
        void parseFailure_returnsEmptyLists() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            c.setEndpoints("[{invalid json}");
            c.setPricing("[{invalid json}");
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));
            when(providerRepository.findByCode("openai")).thenReturn(Optional.empty());

            PlanDetailResponse result = service.getPlanDetail("p-1");

            assertThat(result.getEndpoints()).isEmpty();
            assertThat(result.getPricing()).isEmpty();
        }
    }

    // ==================== getPricing 测试 ====================

    @Nested
    @DisplayName("getPricing 定价查询")
    class GetPricingTests {

        @Test
        @DisplayName("套餐不存在时抛 PLAN_NOT_FOUND")
        void planNotFound_throws() {
            when(planCatalogRepository.findByPlanCode("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPricing("nope"))
                    .isInstanceOf(CatalogException.class)
                    .satisfies(ex -> assertThat(((CatalogException) ex).getCode())
                            .isEqualTo("PLAN_NOT_FOUND"));
        }

        @Test
        @DisplayName("解析成功返回定价列表")
        void parseSuccess() {
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            c.setPricing("[{\"providerModelId\":\"gpt-4\",\"inputPrice\":3.5}]");
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));

            List<PlanDetailResponse.PricingInfo> result = service.getPricing("p-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderModelId()).isEqualTo("gpt-4");
            assertThat(result.get(0).getOutputPrice()).isNull();
        }
    }

    // ==================== listModels 测试 ====================

    @Nested
    @DisplayName("listModels 模型目录查询")
    class ListModelsTests {

        @Test
        @DisplayName("keyword 优先：非空 keyword 走 findByKeyword，不查 capability/findAll")
        void listModels_keywordPriority_usesFindByKeyword() {
            Model m = model("gpt-4", true);
            when(modelRepository.findByKeyword("gpt")).thenReturn(List.of(m));

            List<ModelResponse> result = service.listModels(null, "gpt", null);

            assertThat(result).hasSize(1);
            verify(modelRepository, never()).findByCapability(any());
            verify(modelRepository, never()).findAll();
        }

        @Test
        @DisplayName("capability 次之：keyword 为空时按能力查询")
        void listModels_capabilityFallback_usesFindByCapability() {
            Model m = model("gpt-4", true);
            when(modelRepository.findByCapability("vision")).thenReturn(List.of(m));

            List<ModelResponse> result = service.listModels(null, null, "vision");

            assertThat(result).hasSize(1);
            verify(modelRepository, never()).findByKeyword(any());
            verify(modelRepository, never()).findAll();
        }

        @Test
        @DisplayName("keyword 与 capability 均为空时查询全部")
        void listModels_noCriteria_usesFindAll() {
            Model m = model("gpt-4", true);
            when(modelRepository.findAll()).thenReturn(List.of(m));

            List<ModelResponse> result = service.listModels(null, null, null);

            assertThat(result).hasSize(1);
            verify(modelRepository, never()).findByKeyword(any());
            verify(modelRepository, never()).findByCapability(any());
        }

        @Test
        @DisplayName("providerCode 非空时按套餐关联二次过滤")
        void providerCodeFilter() {
            Model m1 = model("gpt-4", true);
            Model m2 = model("claude-3", true);
            when(modelRepository.findAll()).thenReturn(List.of(m1, m2));

            PlanModelCatalog pm = new PlanModelCatalog();
            pm.setPlanCode("p-1");
            pm.setModelName("gpt-4");
            PlanCatalog c = plan("p-1", "openai", "Plan 1", BillingMode.PAY_AS_YOU_GO);
            when(planModelCatalogRepository.findAll()).thenReturn(List.of(pm));
            when(planCatalogRepository.findByProviderCode("openai")).thenReturn(List.of(c));
            when(planModelCatalogRepository.findByModelName("gpt-4")).thenReturn(List.of(pm));
            when(planCatalogRepository.findByPlanCode("p-1")).thenReturn(Optional.of(c));

            List<ModelResponse> result = service.listModels("openai", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModelName()).isEqualTo("gpt-4");
            assertThat(result.get(0).getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("不可用模型被过滤")
        void unavailableFiltered() {
            Model unavailable = model("gpt-4", false);
            when(modelRepository.findAll()).thenReturn(List.of(unavailable));

            List<ModelResponse> result = service.listModels(null, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("能力提取：仅保留 value=true 的能力键")
        void extractCapabilities() {
            Model m = new Model();
            m.setModelName("gpt-4");
            m.setCapabilities(Map.of("vision", false, "function_calling", true));
            m.setContextWindow(8000);
            m.setMaxOutputTokens(4096);
            when(modelRepository.findAll()).thenReturn(List.of(m));
            // 供应商反查：关联为空 → providerCode null
            when(planModelCatalogRepository.findByModelName("gpt-4")).thenReturn(List.of());

            List<ModelResponse> result = service.listModels(null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCapabilities()).containsExactly("function_calling");
            assertThat(result.get(0).getProviderCode()).isNull();
            assertThat(result.get(0).getContextWindow()).isEqualTo(8000);
            assertThat(result.get(0).getMaxOutputTokens()).isEqualTo(4096);
            assertThat(result.get(0).getMaterialized()).isTrue();
        }

        @Test
        @DisplayName("供应商反查：有关联但套餐不存在时返回 null")
        void providerCodeFallback_nullWhenPlanMissing() {
            Model m = model("gpt-4", true);
            when(modelRepository.findAll()).thenReturn(List.of(m));
            PlanModelCatalog pm = new PlanModelCatalog();
            pm.setPlanCode("p-missing");
            pm.setModelName("gpt-4");
            when(planModelCatalogRepository.findByModelName("gpt-4")).thenReturn(List.of(pm));
            when(planCatalogRepository.findByPlanCode("p-missing")).thenReturn(Optional.empty());

            List<ModelResponse> result = service.listModels(null, null, null);

            assertThat(result.get(0).getProviderCode()).isNull();
        }
    }

    // ==================== 辅助方法 ====================

    private Provider provider(String code, String name) {
        Provider p = new Provider();
        p.setCode(code);
        p.setName(name);
        return p;
    }

    private PlanCatalog plan(String planCode, String providerCode, String planName, BillingMode billingMode) {
        PlanCatalog c = new PlanCatalog();
        c.setPlanCode(planCode);
        c.setProviderCode(providerCode);
        c.setPlanName(planName);
        c.setBillingMode(billingMode);
        return c;
    }

    private Model model(String modelName, boolean available) {
        Model m = new Model();
        m.setModelName(modelName);
        m.setDisplayName(modelName);
        if (!available) {
            m.setDeprecatedAt(java.time.Instant.now());
        }
        return m;
    }
}
