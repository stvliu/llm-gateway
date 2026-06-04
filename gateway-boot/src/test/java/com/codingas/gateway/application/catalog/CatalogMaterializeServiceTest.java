package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.MaterializeBatchRequest;
import com.codingas.gateway.application.catalog.dto.MaterializePlanRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeResult;
import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CatalogMaterializeService 单元测试
 *
 * <p>覆盖物化供应商、套餐、模型的完整流程，以及扩展版物化（批量 API Key）。</p>
 */
@ExtendWith(MockitoExtension.class)
class CatalogMaterializeServiceTest {

    @Mock
    private ProviderCatalogGateway providerCatalogGateway;
    @Mock
    private ModelCatalogGateway modelCatalogGateway;
    @Mock
    private ProviderGateway providerGateway;
    @Mock
    private ModelGateway modelGateway;
    @Mock
    private PlanCatalogGateway planCatalogGateway;
    @Mock
    private com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway planModelCatalogGateway;
    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ChannelEndpointGateway channelEndpointGateway;
    @Mock
    private ChannelModelGateway channelModelGateway;
    @Mock
    private ChannelCredentialGateway channelCredentialGateway;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private CatalogMaterializeService service;

    // ===== 物化供应商 =====

    @Nested
    @DisplayName("物化供应商")
    class MaterializeProviderTests {

        @Test
        @DisplayName("成功物化供应商")
        void success() {
            when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());
            when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(createProviderCatalog("openai")));
            Provider saved = createTestProvider(1L, "openai");
            when(providerGateway.save(any(Provider.class))).thenReturn(saved);

            MaterializeResult result = service.materializeProvider("openai");

            assertEquals("PROVIDER", result.getType());
            assertEquals("openai", result.getCode());
            assertEquals(1L, result.getEntityId());
            assertEquals("CREATED", result.getStatus());
            verify(providerGateway).save(any(Provider.class));
        }

        @Test
        @DisplayName("供应商已物化时抛异常")
        void alreadyMaterialized() {
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(createTestProvider(1L, "openai")));

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializeProvider("openai"));
            assertEquals("ALREADY_MATERIALIZED", ex.getCode());
            verify(providerGateway, never()).save(any());
        }

        @Test
        @DisplayName("供应商目录不存在时抛异常")
        void catalogNotFound() {
            when(providerGateway.findByCode("unknown")).thenReturn(Optional.empty());
            when(providerCatalogGateway.findByProviderCode("unknown")).thenReturn(Optional.empty());

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializeProvider("unknown"));
            assertEquals("CATALOG_NOT_FOUND", ex.getCode());
        }
    }

    // ===== 物化模型 =====

    @Nested
    @DisplayName("物化模型")
    class MaterializeModelTests {

        @Test
        @DisplayName("成功物化模型")
        void success() {
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.empty());
            when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(createModelCatalog()));
            Model saved = createTestModel(10L, "gpt-4o");
            when(modelGateway.save(any(Model.class))).thenReturn(saved);

            MaterializeResult result = service.materializeModel("gpt-4o");

            assertEquals("MODEL", result.getType());
            assertEquals("gpt-4o", result.getCode());
            assertEquals(10L, result.getEntityId());
            assertEquals("CREATED", result.getStatus());
            verify(modelGateway).save(any(Model.class));
        }

        @Test
        @DisplayName("模型已物化时抛异常")
        void alreadyMaterialized() {
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(createTestModel(10L, "gpt-4o")));

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializeModel("gpt-4o"));
            assertEquals("ALREADY_MATERIALIZED", ex.getCode());
            verify(modelGateway, never()).save(any());
        }

        @Test
        @DisplayName("模型目录不存在时抛异常")
        void catalogNotFound() {
            when(modelGateway.findByModelName("unknown-model")).thenReturn(Optional.empty());
            when(modelCatalogGateway.findByModelName("unknown-model")).thenReturn(Optional.empty());

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializeModel("unknown-model"));
            assertEquals("CATALOG_NOT_FOUND", ex.getCode());
        }
    }

    // ===== 物化套餐（基础版） =====

    @Nested
    @DisplayName("物化套餐 - 基础版")
    class MaterializePlanTests {

        @Test
        @DisplayName("套餐不存在时抛异常")
        void catalogNotFound() {
            when(planCatalogGateway.findByPlanCode("unknown-plan")).thenReturn(Optional.empty());

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializePlan("unknown-plan"));
            assertEquals("CATALOG_NOT_FOUND", ex.getCode());
        }

        @Test
        @DisplayName("供应商未物化时抛异常")
        void providerNotMaterialized() {
            PlanCatalog plan = createOpenaiStandardPlan();
            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializePlan("openai_standard"));
            assertEquals("PROVIDER_NOT_MATERIALIZED", ex.getCode());
        }

        @Test
        @DisplayName("套餐已物化时抛异常")
        void alreadyMaterialized() {
            PlanCatalog plan = createOpenaiStandardPlan();
            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(createTestProvider(1L, "openai")));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(true);

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializePlan("openai_standard"));
            assertEquals("ALREADY_MATERIALIZED", ex.getCode());
        }

        @Test
        @DisplayName("成功物化套餐——创建渠道、端点、渠道模型")
        void success() {
            PlanCatalog plan = createOpenaiStandardPlan();
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(false);

            Channel savedChannel = createTestChannel(13L);
            when(channelGateway.save(any(Channel.class))).thenReturn(savedChannel);
            when(channelEndpointGateway.save(any(ChannelEndpoint.class))).thenReturn(new ChannelEndpoint());

            // 模型已在目录中存在，但运营实体不存在 → 级联物化
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.empty());
            ModelCatalog modelCatalog = createModelCatalog("gpt-4o");
            when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(modelCatalog));
            Model savedModel = createTestModel(4L, "gpt-4o");
            when(modelGateway.save(any(Model.class))).thenReturn(savedModel);
            when(channelModelGateway.save(any(ChannelModel.class))).thenReturn(new ChannelModel());

            MaterializeResult result = service.materializePlan("openai_standard");

            assertEquals("PLAN", result.getType());
            assertEquals("openai_standard", result.getCode());
            assertEquals(13L, result.getEntityId());
            assertEquals("CREATED", result.getStatus());

            // 验证创建了 1 个端点
            verify(channelEndpointGateway, times(1)).save(any(ChannelEndpoint.class));
            // 验证创建了 1 个渠道模型
            verify(channelModelGateway, times(1)).save(any(ChannelModel.class));
            // 验证级联物化了 1 个模型
            verify(modelGateway, times(1)).save(any(Model.class));
        }

        @Test
        @DisplayName("物化套餐时上游模型名预填——规则命中")
        void upstreamModelName_prefilledWhenRulesMatch() {
            PlanCatalog plan = createPlanWithPricing("aws-bedrock-standard", "aws-bedrock",
                    List.of(Map.of("providerModelId", "claude-opus-4-7", "inputPrice", BigDecimal.valueOf(15), "outputPrice", BigDecimal.valueOf(75))));
            Provider provider = createTestProvider(1L, "aws-bedrock");

            when(planCatalogGateway.findByPlanCode("aws-bedrock-standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("aws-bedrock")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "aws-bedrock-standard")).thenReturn(false);
            when(channelGateway.save(any())).thenReturn(createTestChannel(1L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(1L, "claude-opus-4-7");
            when(modelGateway.findByModelName("claude-opus-4-7")).thenReturn(Optional.of(existingModel));

            service.materializePlan("aws-bedrock-standard");

            ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
            verify(channelModelGateway).save(captor.capture());
            assertEquals("anthropic.claude-opus-4-7", captor.getValue().getUpstreamModelName());
        }

        @Test
        @DisplayName("物化套餐时上游模型名预填——规则未命中返回 null")
        void upstreamModelName_nullWhenNoRuleMatches() {
            PlanCatalog plan = createPlanWithPricing("deepseek-standard", "deepseek",
                    List.of(Map.of("providerModelId", "deepseek-chat", "inputPrice", BigDecimal.valueOf(1), "outputPrice", BigDecimal.valueOf(4))));
            Provider provider = createTestProvider(1L, "deepseek");

            when(planCatalogGateway.findByPlanCode("deepseek-standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "deepseek-standard")).thenReturn(false);
            when(channelGateway.save(any())).thenReturn(createTestChannel(2L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(2L, "deepseek-chat");
            when(modelGateway.findByModelName("deepseek-chat")).thenReturn(Optional.of(existingModel));

            service.materializePlan("deepseek-standard");

            ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
            verify(channelModelGateway).save(captor.capture());
            assertNull(captor.getValue().getUpstreamModelName());
        }

        @Test
        @DisplayName("pricing 中 providerModelId 为 null 时抛异常")
        void nullModelNameThrowsException() {
            // Map.of 不允许 null 值，使用 HashMap
            java.util.Map<String, Object> pricingEntry = new java.util.HashMap<>();
            pricingEntry.put("providerModelId", null);
            pricingEntry.put("inputPrice", BigDecimal.valueOf(1));
            PlanCatalog plan = createPlanWithPricing("bad-plan", "openai", List.of(pricingEntry));
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("bad-plan")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "bad-plan")).thenReturn(false);
            when(channelGateway.save(any())).thenReturn(createTestChannel(1L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            CatalogException ex = assertThrows(CatalogException.class, () -> service.materializePlan("bad-plan"));
            assertEquals("INVALID_CATALOG_DATA", ex.getCode());
        }
    }

    // ===== 物化套餐（扩展版——批量 API Key） =====

    @Nested
    @DisplayName("物化套餐 - 扩展版（批量 API Key）")
    class MaterializePlanExtendedTests {

        @Test
        @DisplayName("成功物化套餐并批量创建 API Key 凭证")
        void successWithApiKeys() {
            PlanCatalog plan = createOpenaiStandardPlan();
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(false);
            when(channelGateway.save(any(Channel.class))).thenReturn(createTestChannel(13L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(4L, "gpt-4o");
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existingModel));
            when(channelModelGateway.save(any())).thenReturn(new ChannelModel());

            // 凭证保存返回
            ChannelCredential savedCred = new ChannelCredential();
            savedCred.setId(100L);
            savedCred.setChannelId(13L);
            when(channelCredentialGateway.save(any(ChannelCredential.class))).thenReturn(savedCred);

            MaterializePlanRequest request = new MaterializePlanRequest();
            request.setApiKeys(List.of("sk-key-abc123456", "sk-key-def789012"));

            MaterializeResult result = service.materializePlan("openai_standard", request);

            assertEquals("CREATED", result.getStatus());
            assertEquals(13L, result.getEntityId());

            // 验证创建了 2 个凭证
            ArgumentCaptor<ChannelCredential> credCaptor = ArgumentCaptor.forClass(ChannelCredential.class);
            verify(channelCredentialGateway, times(2)).save(credCaptor.capture());

            List<ChannelCredential> credentials = credCaptor.getAllValues();
            // sk-key-abc123456 的前 8 位是 sk-key-a
            assertEquals("sk-key-a", credentials.get(0).getApiKeyPrefix());
            assertEquals(1, credentials.get(0).getPriority());
            assertEquals(2, credentials.get(1).getPriority());
            assertEquals(CredentialState.ACTIVE, credentials.get(0).getState());
            assertEquals(CredentialState.ACTIVE, credentials.get(1).getState());
        }

        @Test
        @DisplayName("批量 API Key 中过滤 null 和空白")
        void filterNullAndBlankApiKeys() {
            PlanCatalog plan = createOpenaiStandardPlan();
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(false);
            when(channelGateway.save(any(Channel.class))).thenReturn(createTestChannel(13L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(4L, "gpt-4o");
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existingModel));
            when(channelModelGateway.save(any())).thenReturn(new ChannelModel());

            ChannelCredential savedCred = new ChannelCredential();
            savedCred.setId(100L);
            when(channelCredentialGateway.save(any(ChannelCredential.class))).thenReturn(savedCred);

            MaterializePlanRequest request = new MaterializePlanRequest();
            request.setApiKeys(java.util.Arrays.asList("sk-valid-key-123456", null, "", "  ", "sk-another-key-789012"));

            service.materializePlan("openai_standard", request);

            // 只有 2 个有效 Key 被保存
            verify(channelCredentialGateway, times(2)).save(any(ChannelCredential.class));
        }

        @Test
        @DisplayName("无 API Key 时仅完成基础物化")
        void noApiKeys() {
            PlanCatalog plan = createOpenaiStandardPlan();
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(false);
            when(channelGateway.save(any(Channel.class))).thenReturn(createTestChannel(13L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(4L, "gpt-4o");
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existingModel));
            when(channelModelGateway.save(any())).thenReturn(new ChannelModel());

            MaterializePlanRequest request = new MaterializePlanRequest();
            // apiKeys 为 null

            MaterializeResult result = service.materializePlan("openai_standard", request);

            assertEquals("CREATED", result.getStatus());
            // 不应创建任何凭证
            verify(channelCredentialGateway, never()).save(any());
        }

        @Test
        @DisplayName("套餐已物化时直接返回，不创建凭证")
        void alreadyMaterialized_returnsExisting() {
            when(planCatalogGateway.findByPlanCode("openai_standard"))
                    .thenReturn(Optional.of(createOpenaiStandardPlan()));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(createTestProvider(1L, "openai")));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(true);

            CatalogException ex = assertThrows(CatalogException.class,
                    () -> service.materializePlan("openai_standard", new MaterializePlanRequest()));
            assertEquals("ALREADY_MATERIALIZED", ex.getCode());
        }

        @Test
        @DisplayName("request 为 null 时仅完成基础物化")
        void nullRequest() {
            PlanCatalog plan = createOpenaiStandardPlan();
            Provider provider = createTestProvider(1L, "openai");

            when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(plan));
            when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));
            when(channelGateway.existsByProviderIdAndName(1L, "openai_standard")).thenReturn(false);
            when(channelGateway.save(any(Channel.class))).thenReturn(createTestChannel(13L));
            when(channelEndpointGateway.save(any())).thenReturn(new ChannelEndpoint());

            Model existingModel = createTestModel(4L, "gpt-4o");
            when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(existingModel));
            when(channelModelGateway.save(any())).thenReturn(new ChannelModel());

            // request 为 null，扩展版直接调用基础版
            MaterializeResult result = service.materializePlan("openai_standard", (MaterializePlanRequest) null);

            assertEquals("CREATED", result.getStatus());
            assertEquals(13L, result.getEntityId());
            verify(channelCredentialGateway, never()).save(any());
        }
    }

    // ===== 级联物化供应商（含关联 Plans） =====

    @Nested
    @DisplayName("级联物化供应商")
    class MaterializeProviderWithPlansTests {

        @Test
        @DisplayName("供应商不存在时抛异常")
        void nonExistentProvider() {
            when(providerGateway.findByCode("nonexistent")).thenReturn(Optional.empty());
            when(providerCatalogGateway.findByProviderCode("nonexistent")).thenReturn(Optional.empty());

            CatalogException ex = assertThrows(CatalogException.class,
                    () -> service.materializeProviderWithPlans("nonexistent", null));
            assertEquals("CATALOG_NOT_FOUND", ex.getCode());
        }

        @Test
        @DisplayName("级联物化所有 Plans")
        void cascadeAll() {
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerCatalogGateway.findByProviderCode("deepseek")).thenReturn(Optional.of(createProviderCatalog("deepseek")));
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.empty());
            when(providerGateway.save(any(Provider.class))).thenReturn(savedProvider);

            PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
            PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2));

            MaterializeResult r1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
            MaterializeResult r2 = MaterializeResult.builder().type("PLAN").code("deepseek_pro").entityId(3L).status("CREATED").build();
            doReturn(r1).when(service).materializePlan("deepseek_std");
            doReturn(r2).when(service).materializePlan("deepseek_pro");

            var result = service.materializeProviderWithPlans("deepseek", null);

            assertEquals(2, result.getTotalCount());
            assertEquals(2, result.getSuccessCount());
            assertEquals(0, result.getFailedCount());
            assertEquals("CREATED", result.getResults().get(0).getStatus());
        }

        @Test
        @DisplayName("指定物化部分 Plans")
        void cascadeSpecificPlans() {
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

            PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
            PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
            PlanCatalog plan3 = createPlanCatalog("deepseek", "deepseek_ultra", CatalogState.ACTIVE);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2, plan3));

            MaterializeResult r1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
            MaterializeResult r2 = MaterializeResult.builder().type("PLAN").code("deepseek_pro").entityId(3L).status("CREATED").build();
            doReturn(r1).when(service).materializePlan("deepseek_std");
            doReturn(r2).when(service).materializePlan("deepseek_pro");

            MaterializeBatchRequest request = new MaterializeBatchRequest();
            request.setPlanCodes(List.of("deepseek_std", "deepseek_pro"));
            var result = service.materializeProviderWithPlans("deepseek", request);

            assertEquals(2, result.getTotalCount());
            verify(service, never()).materializePlan("deepseek_ultra");
        }

        @Test
        @DisplayName("所有 Plans 已物化时全部跳过")
        void allPlansAlreadyMaterialized() {
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

            PlanCatalog plan = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan));

            doThrow(new CatalogException("ALREADY_MATERIALIZED", "套餐已物化")).when(service).materializePlan("deepseek_std");

            var result = service.materializeProviderWithPlans("deepseek", null);

            assertEquals(1, result.getTotalCount());
            assertEquals(1, result.getSkippedCount());
            assertEquals("SKIPPED", result.getResults().get(0).getStatus());
        }

        @Test
        @DisplayName("供应商已存在时不重复创建")
        void providerAlreadyExists() {
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

            PlanCatalog plan = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan));

            MaterializeResult planResult = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(5L).status("CREATED").build();
            doReturn(planResult).when(service).materializePlan("deepseek_std");

            var result = service.materializeProviderWithPlans("deepseek", null);

            assertEquals(1, result.getSuccessCount());
            verify(providerGateway, never()).save(any(Provider.class));
        }

        @Test
        @DisplayName("无关联 Plans 时返回空结果")
        void noPlans() {
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.empty());
            when(providerCatalogGateway.findByProviderCode("deepseek")).thenReturn(Optional.of(createProviderCatalog("deepseek")));
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerGateway.save(any(Provider.class))).thenReturn(savedProvider);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of());

            var result = service.materializeProviderWithPlans("deepseek", null);

            assertEquals(0, result.getTotalCount());
            assertTrue(result.getResults().isEmpty());
        }

        @Test
        @DisplayName("部分失败时正确统计")
        void partialFailure() {
            Provider savedProvider = createTestProvider(1L, "deepseek");
            when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

            PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
            PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
            when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2));

            MaterializeResult r1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
            doReturn(r1).when(service).materializePlan("deepseek_std");
            doThrow(new RuntimeException("JSON parse error")).when(service).materializePlan("deepseek_pro");

            var result = service.materializeProviderWithPlans("deepseek", null);

            assertEquals(1, result.getSuccessCount());
            assertEquals(1, result.getFailedCount());
            assertEquals("CREATED", result.getResults().get(0).getStatus());
            assertEquals("FAILED", result.getResults().get(1).getStatus());
            assertEquals("JSON parse error", result.getResults().get(1).getErrorMessage());
        }
    }

    // ===== 辅助方法 =====

    private ProviderCatalog createProviderCatalog(String code) {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode(code);
        catalog.setProviderName(code);
        catalog.setWebsiteUrl("https://example.com");
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private ModelCatalog createModelCatalog() {
        return createModelCatalog("gpt-4o");
    }

    private ModelCatalog createModelCatalog(String modelName) {
        ModelCatalog catalog = new ModelCatalog();
        catalog.setModelName(modelName);
        catalog.setDisplayName(modelName);
        catalog.setModelFamily("gpt-4");
        catalog.setContextWindow(128000);
        catalog.setMaxOutputTokens(16384);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private Provider createTestProvider(Long id, String code) {
        Provider p = new Provider();
        p.setId(id);
        p.setCode(code);
        p.setName(code);
        p.setState(ProviderState.ACTIVE);
        return p;
    }

    private Model createTestModel(Long id, String modelName) {
        Model m = new Model();
        m.setId(id);
        m.setModelName(modelName);
        m.setDisplayName(modelName);
        m.setState(ModelState.ACTIVE);
        return m;
    }

    private Channel createTestChannel(Long id) {
        Channel c = new Channel();
        c.setId(id);
        c.setState(ChannelState.ACTIVE);
        return c;
    }

    /** 创建 openai_standard 套餐目录（含 1 个端点 + 1 个模型） */
    private PlanCatalog createOpenaiStandardPlan() {
        PlanCatalog p = new PlanCatalog();
        p.setPlanCode("openai_standard");
        p.setProviderCode("openai");
        p.setPlanName("按量付费");
        p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.openai.com/v1\"}]");
        p.setPricing("[{\"providerModelId\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10.0,\"cacheReadPrice\":1.25}]");
        p.setSource(CatalogSource.BUILTIN);
        p.setState(CatalogState.ACTIVE);
        return p;
    }

    private PlanCatalog createPlanCatalog(String providerCode, String planCode, CatalogState state) {
        PlanCatalog p = new PlanCatalog();
        p.setProviderCode(providerCode);
        p.setPlanCode(planCode);
        p.setPlanName(planCode);
        p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.example.com\"}]");
        p.setPricing("[{\"providerModelId\":\"deepseek-chat\",\"inputPrice\":1.0,\"outputPrice\":4.0}]");
        p.setSource(CatalogSource.BUILTIN);
        p.setState(state);
        return p;
    }

    /** 创建带自定义 pricing 的套餐目录（ObjectMapper 为真实实例，自动解析） */
    private PlanCatalog createPlanWithPricing(String planCode, String providerCode, List<Map<String, Object>> pricing) {
        PlanCatalog p = new PlanCatalog();
        p.setPlanCode(planCode);
        p.setProviderCode(providerCode);
        p.setPlanName(planCode);
        p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.example.com/v1\"}]");
        try {
            p.setPricing(objectMapper.writeValueAsString(pricing));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        p.setSource(CatalogSource.BUILTIN);
        p.setState(CatalogState.ACTIVE);
        return p;
    }
}