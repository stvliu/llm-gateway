package com.codingas.gateway.application.catalog;

import com.codingas.gateway.application.catalog.dto.MaterializeBatchRequest;
import com.codingas.gateway.application.catalog.dto.MaterializeBatchResult;
import com.codingas.gateway.application.catalog.dto.MaterializeResult;
import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
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
    private com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway planCatalogGateway;

    @Mock
    private com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway planModelCatalogGateway;

    @Mock
    private com.codingas.gateway.domain.supply.gateway.ChannelGateway channelGateway;

    @Mock
    private com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway channelEndpointGateway;

    @Mock
    private com.codingas.gateway.domain.supply.gateway.ChannelModelGateway channelModelGateway;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Spy
    @InjectMocks
    private CatalogMaterializeService service;

    @Test
    void materializeProvider_success() {
        // 供应商尚未物化
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        // 目录存在
        ProviderCatalog catalog = createProviderCatalog();
        when(providerCatalogGateway.findByProviderCode("openai")).thenReturn(Optional.of(catalog));

        // 保存成功
        Provider saved = createProvider();
        saved.setId(1L);
        when(providerGateway.save(any(Provider.class))).thenReturn(saved);

        var result = service.materializeProvider("openai");

        assertNotNull(result);
        assertEquals("PROVIDER", result.getType());
        assertEquals("openai", result.getCode());
        assertEquals(1L, result.getEntityId());
        assertEquals("CREATED", result.getStatus());
        verify(providerGateway).save(any(Provider.class));
    }

    @Test
    void materializeProvider_alreadyMaterialized_throwsException() {
        // 供应商已物化
        when(providerGateway.findByCode("openai")).thenReturn(Optional.of(createProvider()));

        assertThrows(CatalogException.class, () -> service.materializeProvider("openai"));

        verify(providerGateway, never()).save(any());
    }

    @Test
    void materializeProvider_catalogNotFound_throwsException() {
        // 供应商尚未物化
        when(providerGateway.findByCode("unknown")).thenReturn(Optional.empty());

        // 目录不存在
        when(providerCatalogGateway.findByProviderCode("unknown")).thenReturn(Optional.empty());

        assertThrows(CatalogException.class, () -> service.materializeProvider("unknown"));
    }

    @Test
    void materializeModel_success() {
        // Model 尚未物化
        when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.empty());

        // 目录存在
        ModelCatalog catalog = createModelCatalog();
        when(modelCatalogGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(catalog));

        // 保存成功
        Model saved = createModel();
        saved.setId(10L);
        when(modelGateway.save(any(Model.class))).thenReturn(saved);

        var result = service.materializeModel("gpt-4o");

        assertNotNull(result);
        assertEquals("MODEL", result.getType());
        assertEquals("gpt-4o", result.getCode());
        assertEquals(10L, result.getEntityId());
        assertEquals("CREATED", result.getStatus());
        verify(modelGateway).save(any(Model.class));
    }

    @Test
    void materializeModel_alreadyMaterialized_throwsException() {
        // Model 已物化
        when(modelGateway.findByModelName("gpt-4o")).thenReturn(Optional.of(createModel()));

        assertThrows(CatalogException.class, () -> service.materializeModel("gpt-4o"));

        verify(modelGateway, never()).save(any());
    }

    @Test
    void shouldPrefillUpstreamModelNameWhenRulesMatch() throws Exception {
        // Prepare azure-openai data
        PlanCatalog plan = new PlanCatalog();
        plan.setPlanCode("azure-openai-standard");
        plan.setProviderCode("azure-openai");
        plan.setBillingMode(com.codingas.gateway.domain.supply.catalog.enums.BillingMode.PAY_AS_YOU_GO);
        plan.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://azure.openai.com\"}]");
        plan.setPricing("[{\"modelName\":\"chat-latest\",\"inputPrice\":2.5,\"outputPrice\":10.0}]");

        when(planCatalogGateway.findByPlanCode("azure-openai-standard")).thenReturn(Optional.of(plan));
        when(providerGateway.findByCode("azure-openai")).thenReturn(Optional.of(createTestProvider("azure-openai")));
        when(channelGateway.existsByProviderIdAndName(anyLong(), eq("azure-openai-standard"))).thenReturn(false);
        when(channelGateway.save(any())).thenReturn(createTestChannel(1L));

        // objectMapper 模拟：解析 endpoints
        when(objectMapper.readValue(eq("[{\"protocol\":\"OPENAI\",\"url\":\"https://azure.openai.com\"}]"), any(TypeReference.class)))
                .thenReturn(List.of(Map.of("protocol", "OPENAI", "url", "https://azure.openai.com")));
        // objectMapper 模拟：解析 pricing
        when(objectMapper.readValue(eq("[{\"modelName\":\"chat-latest\",\"inputPrice\":2.5,\"outputPrice\":10.0}]"), any(TypeReference.class)))
                .thenReturn(List.of(Map.of("modelName", "chat-latest", "inputPrice", BigDecimal.valueOf(2.5), "outputPrice", BigDecimal.valueOf(10.0))));

        Model model = new Model();
        model.setId(1L);
        model.setModelName("chat-latest");
        when(modelGateway.findByModelName("chat-latest")).thenReturn(Optional.of(model));

        service.materializePlan("azure-openai-standard");

        ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
        verify(channelModelGateway, times(1)).save(captor.capture());
        ChannelModel saved = captor.getValue();
        assertEquals("gpt-chat-latest", saved.getUpstreamModelName());
    }

    @Test
    void shouldSetNullUpstreamModelNameWhenNoRuleMatches() throws Exception {
        PlanCatalog plan = new PlanCatalog();
        plan.setPlanCode("deepseek-standard");
        plan.setProviderCode("deepseek");
        plan.setBillingMode(com.codingas.gateway.domain.supply.catalog.enums.BillingMode.PAY_AS_YOU_GO);
        plan.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.deepseek.com\"}]");
        plan.setPricing("[{\"modelName\":\"deepseek-v4-flash\",\"inputPrice\":1.0,\"outputPrice\":4.0}]");

        when(planCatalogGateway.findByPlanCode("deepseek-standard")).thenReturn(Optional.of(plan));
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(createTestProvider("deepseek")));
        when(channelGateway.existsByProviderIdAndName(anyLong(), eq("deepseek-standard"))).thenReturn(false);
        when(channelGateway.save(any())).thenReturn(createTestChannel(2L));

        // objectMapper 模拟：解析 endpoints
        when(objectMapper.readValue(eq("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.deepseek.com\"}]"), any(TypeReference.class)))
                .thenReturn(List.of(Map.of("protocol", "OPENAI", "url", "https://api.deepseek.com")));
        // objectMapper 模拟：解析 pricing
        when(objectMapper.readValue(eq("[{\"modelName\":\"deepseek-v4-flash\",\"inputPrice\":1.0,\"outputPrice\":4.0}]"), any(TypeReference.class)))
                .thenReturn(List.of(Map.of("modelName", "deepseek-v4-flash", "inputPrice", BigDecimal.valueOf(1.0), "outputPrice", BigDecimal.valueOf(4.0))));

        Model model = new Model();
        model.setId(2L);
        model.setModelName("deepseek-v4-flash");
        when(modelGateway.findByModelName("deepseek-v4-flash")).thenReturn(Optional.of(model));

        service.materializePlan("deepseek-standard");

        ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
        verify(channelModelGateway, times(1)).save(captor.capture());
        ChannelModel saved = captor.getValue();
        assertNull(saved.getUpstreamModelName());
    }

    @Test
    void materializeProviderWithPlans_cascadeAll() throws Exception {
        // 供应商尚未物化 → 物化 Provider
        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);

        when(providerCatalogGateway.findByProviderCode("deepseek")).thenReturn(Optional.of(createProviderCatalog("deepseek")));
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.empty());
        when(providerGateway.save(any(Provider.class))).thenReturn(savedProvider);

        // 关联 2 个 ACTIVE Plans
        PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
        PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2));

        // 短路 materializePlan：模拟成功
        MaterializeResult planResult1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
        MaterializeResult planResult2 = MaterializeResult.builder().type("PLAN").code("deepseek_pro").entityId(3L).status("CREATED").build();
        doReturn(planResult1).when(service).materializePlan("deepseek_std");
        doReturn(planResult2).when(service).materializePlan("deepseek_pro");

        // 执行级联物化
        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", null);

        // 验证
        assertNotNull(result);
        assertEquals("deepseek", result.getProviderCode());
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(2, result.getResults().size());
        assertEquals("CREATED", result.getResults().get(0).getStatus());
        assertEquals("CREATED", result.getResults().get(1).getStatus());

        verify(providerGateway).save(any(Provider.class));
    }

    @Test
    void materializeProviderWithPlans_cascadeSpecificPlans() throws Exception {
        // 供应商已存在
        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

        // 关联 3 个 Plans（只物化指定的 2 个）
        PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
        PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
        PlanCatalog plan3 = createPlanCatalog("deepseek", "deepseek_ultra", CatalogState.ACTIVE);
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2, plan3));

        // 短路 materializePlan
        MaterializeResult planResult1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
        MaterializeResult planResult2 = MaterializeResult.builder().type("PLAN").code("deepseek_pro").entityId(3L).status("CREATED").build();
        doReturn(planResult1).when(service).materializePlan("deepseek_std");
        doReturn(planResult2).when(service).materializePlan("deepseek_pro");

        // 执行级联（只物化 2 个指定 plan）
        MaterializeBatchRequest request = new MaterializeBatchRequest();
        request.setPlanCodes(List.of("deepseek_std", "deepseek_pro"));
        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", request);

        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(2, result.getResults().size());

        // 验证第三个 Plan 没有被物化
        verify(service, never()).materializePlan("deepseek_ultra");
    }

    @Test
    void materializeProviderWithPlans_allPlansAlreadyMaterialized() throws Exception {
        // 供应商已存在
        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

        // 关联 1 个 Plan
        PlanCatalog plan = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan));

        // 短路 materializePlan → 抛 ALREADY_MATERIALIZED
        doThrow(new CatalogException("ALREADY_MATERIALIZED", "套餐已物化")).when(service).materializePlan("deepseek_std");

        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", null);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals("SKIPPED", result.getResults().get(0).getStatus());
        verify(providerGateway, never()).save(any(Provider.class));
    }

    @Test
    void materializeProviderWithPlans_providerAlreadyExists() throws Exception {
        // 供应商已存在
        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

        // 有 1 个 Plan 待物化
        PlanCatalog plan = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan));

        // 短路 materializePlan → 成功
        MaterializeResult planResult = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(5L).status("CREATED").build();
        doReturn(planResult).when(service).materializePlan("deepseek_std");

        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", null);

        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        // Provider 已存在，不应再次调用 save
        verify(providerGateway, never()).save(any(Provider.class));
    }

    @Test
    void materializeProviderWithPlans_noPlans_returnsEmpty() throws Exception {
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.empty());

        ProviderCatalog catalog = createProviderCatalog("deepseek");
        when(providerCatalogGateway.findByProviderCode("deepseek")).thenReturn(Optional.of(catalog));

        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);
        when(providerGateway.save(any(Provider.class))).thenReturn(savedProvider);

        // 无关联 Plans
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of());

        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", null);

        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertTrue(result.getResults().isEmpty());
        verify(providerGateway).save(any(Provider.class));
    }

    @Test
    void materializeProviderWithPlans_partialFailure() throws Exception {
        // 供应商已存在
        Provider savedProvider = new Provider();
        savedProvider.setId(1L);
        savedProvider.setCode("deepseek");
        savedProvider.setState(ProviderState.ACTIVE);
        when(providerGateway.findByCode("deepseek")).thenReturn(Optional.of(savedProvider));

        PlanCatalog plan1 = createPlanCatalog("deepseek", "deepseek_std", CatalogState.ACTIVE);
        PlanCatalog plan2 = createPlanCatalog("deepseek", "deepseek_pro", CatalogState.ACTIVE);
        when(planCatalogGateway.findByProviderCode("deepseek")).thenReturn(List.of(plan1, plan2));

        // plan1 成功，plan2 失败
        MaterializeResult planResult1 = MaterializeResult.builder().type("PLAN").code("deepseek_std").entityId(2L).status("CREATED").build();
        doReturn(planResult1).when(service).materializePlan("deepseek_std");
        doThrow(new RuntimeException("JSON parse error")).when(service).materializePlan("deepseek_pro");

        MaterializeBatchResult result = service.materializeProviderWithPlans("deepseek", null);

        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("CREATED", result.getResults().get(0).getStatus());
        assertEquals("FAILED", result.getResults().get(1).getStatus());
        assertEquals("JSON parse error", result.getResults().get(1).getErrorMessage());
    }

    // ===== 辅助方法 =====

    private ProviderCatalog createProviderCatalog() {
        return createProviderCatalog("openai");
    }

    private ProviderCatalog createProviderCatalog(String code) {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode(code);
        catalog.setProviderName(code);
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setWebsiteUrl("https://example.com");
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private Provider createProvider() {
        Provider provider = new Provider();
        provider.setCode("openai");
        provider.setName("OpenAI");
        provider.setState(ProviderState.ACTIVE);
        return provider;
    }

    private ModelCatalog createModelCatalog() {
        ModelCatalog catalog = new ModelCatalog();
        catalog.setModelName("gpt-4o");
        catalog.setDisplayName("GPT-4o");
        catalog.setModelFamily("gpt-4");
        catalog.setContextWindow(128000);
        catalog.setMaxOutputTokens(16384);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private Model createModel() {
        Model spec = new Model();
        spec.setModelName("gpt-4o");
        spec.setDisplayName("GPT-4o");
        spec.setState(ModelState.ACTIVE);
        return spec;
    }

    private Provider createTestProvider(String code) {
        Provider p = new Provider();
        p.setId(1L);
        p.setCode(code);
        p.setName(code);
        p.setState(ProviderState.ACTIVE);
        return p;
    }

    private Channel createTestChannel(Long id) {
        Channel c = new Channel();
        c.setId(id);
        c.setState(ChannelState.ACTIVE);
        return c;
    }

    private PlanCatalog createPlanCatalog(String providerCode, String planCode, CatalogState state) {
        PlanCatalog p = new PlanCatalog();
        p.setProviderCode(providerCode);
        p.setPlanCode(planCode);
        p.setPlanName(planCode);
        p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.example.com\"}]");
        p.setPricing("[{\"modelName\":\"deepseek-chat\",\"inputPrice\":1.0,\"outputPrice\":4.0}]");
        p.setSource(CatalogSource.BUILTIN);
        p.setState(state);
        return p;
    }
}