package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.exception.CatalogException;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelSpecCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CatalogMaterializeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CatalogMaterializeServiceTest {

    @Mock
    private ProviderCatalogGateway providerCatalogGateway;

    @Mock
    private ModelSpecCatalogGateway modelSpecCatalogGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelSpecGateway modelSpecGateway;

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
    void materializeModelSpec_success() {
        // ModelSpec 尚未物化
        when(modelSpecGateway.findByProviderModelId("gpt-4o")).thenReturn(Optional.empty());

        // 目录存在
        ModelSpecCatalog catalog = createModelSpecCatalog();
        when(modelSpecCatalogGateway.findByProviderModelId("gpt-4o")).thenReturn(Optional.of(catalog));

        // 反查供应商 ID
        var planModelAssoc = new com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog();
        planModelAssoc.setPlanCode("openai_standard");
        planModelAssoc.setProviderModelId("gpt-4o");
        when(planModelCatalogGateway.findByProviderModelId("gpt-4o")).thenReturn(java.util.List.of(planModelAssoc));

        var planCatalog = new com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog();
        planCatalog.setProviderCode("openai");
        when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(planCatalog));

        Provider provider = createProvider();
        provider.setId(1L);
        when(providerGateway.findByCode("openai")).thenReturn(Optional.of(provider));

        // 保存成功
        ModelSpec saved = createModelSpec();
        saved.setId(10L);
        when(modelSpecGateway.save(any(ModelSpec.class))).thenReturn(saved);

        var result = service.materializeModelSpec("gpt-4o");

        assertNotNull(result);
        assertEquals("MODEL_SPEC", result.getType());
        assertEquals("gpt-4o", result.getCode());
        assertEquals(10L, result.getEntityId());
        assertEquals("CREATED", result.getStatus());
        verify(modelSpecGateway).save(any(ModelSpec.class));
    }

    @Test
    void materializeModelSpec_alreadyMaterialized_throwsException() {
        // ModelSpec 已物化
        when(modelSpecGateway.findByProviderModelId("gpt-4o")).thenReturn(Optional.of(createModelSpec()));

        assertThrows(CatalogException.class, () -> service.materializeModelSpec("gpt-4o"));

        verify(modelSpecGateway, never()).save(any());
    }

    // ===== 辅助方法 =====

    private ProviderCatalog createProviderCatalog() {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode("openai");
        catalog.setProviderName("OpenAI");
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setBaseUrl("https://api.openai.com/v1");
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

    private ModelSpecCatalog createModelSpecCatalog() {
        ModelSpecCatalog catalog = new ModelSpecCatalog();
        catalog.setProviderModelId("gpt-4o");
        catalog.setDisplayName("GPT-4o");
        catalog.setModelFamily("gpt-4");
        catalog.setContextWindow(128000);
        catalog.setMaxOutputTokens(16384);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private ModelSpec createModelSpec() {
        ModelSpec spec = new ModelSpec();
        spec.setProviderModelId("gpt-4o");
        spec.setDisplayName("GPT-4o");
        spec.setState(com.codingas.gateway.domain.supply.enums.ModelSpecState.ACTIVE);
        return spec;
    }
}