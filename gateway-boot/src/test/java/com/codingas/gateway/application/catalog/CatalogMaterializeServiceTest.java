package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
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

    // ===== 辅助方法 =====

    private ProviderCatalog createProviderCatalog() {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode("openai");
        catalog.setProviderName("OpenAI");
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setWebsiteUrl("https://openai.com");
        catalog.setDescription("OpenAI 官方 API");
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
}