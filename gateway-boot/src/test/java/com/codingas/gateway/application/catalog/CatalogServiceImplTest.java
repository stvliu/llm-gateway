package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelSpecCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CatalogServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock
    private ProviderCatalogGateway providerCatalogGateway;

    @Mock
    private PlanCatalogGateway planCatalogGateway;

    @Mock
    private ModelSpecCatalogGateway modelSpecCatalogGateway;

    @Mock
    private PlanModelCatalogGateway planModelCatalogGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelSpecGateway modelSpecGateway;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CatalogServiceImpl service;

    @Test
    void listProviderCatalogs_returnsActiveCatalogs() {
        ProviderCatalog catalog = createProviderCatalog("openai", "OpenAI");
        when(providerCatalogGateway.findAll()).thenReturn(List.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        var result = service.listProviderCatalogs(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("openai", result.get(0).getCode());
        assertEquals("OpenAI", result.get(0).getName());
        assertEquals("INTERNATIONAL", result.get(0).getProviderType());
        assertFalse(result.get(0).getMaterialized());
    }

    @Test
    void listProviderCatalogs_filtersByProviderType() {
        ProviderCatalog catalog = createProviderCatalog("openai", "OpenAI");
        when(providerCatalogGateway.findByProviderType(ProviderType.INTERNATIONAL))
                .thenReturn(List.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        var result = service.listProviderCatalogs("INTERNATIONAL", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("openai", result.get(0).getCode());
    }

    @Test
    void listProviderCatalogs_filtersByKeyword() {
        ProviderCatalog catalog = createProviderCatalog("openai", "OpenAI");
        when(providerCatalogGateway.findByKeyword("open")).thenReturn(List.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        var result = service.listProviderCatalogs(null, "open");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void listProviderCatalogs_showsMaterializedStatus() {
        ProviderCatalog catalog = createProviderCatalog("openai", "OpenAI");
        when(providerCatalogGateway.findAll()).thenReturn(List.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.of(
                new com.codingas.gateway.domain.supply.entity.Provider()));

        var result = service.listProviderCatalogs(null, null);

        assertTrue(result.get(0).getMaterialized());
    }

    @Test
    void listPlanCatalogs_returnsActiveCatalogs() {
        PlanCatalog catalog = createPlanCatalog("openai_standard", "openai");
        when(planCatalogGateway.findAll()).thenReturn(List.of(catalog));
        // isPlanMaterialized 会调用 planCatalogGateway.findByPlanCode
        when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        var result = service.listPlanCatalogs(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("openai_standard", result.get(0).getPlanCode());
        assertEquals("openai", result.get(0).getProviderCode());
    }

    @Test
    void listPlanCatalogs_filtersByProviderCode() {
        PlanCatalog catalog = createPlanCatalog("openai_standard", "openai");
        when(planCatalogGateway.findByProviderCode("openai")).thenReturn(List.of(catalog));
        // isPlanMaterialized 会调用 planCatalogGateway.findByPlanCode
        when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        var result = service.listPlanCatalogs("openai");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPlanDetail_success() {
        PlanCatalog catalog = createPlanCatalog("openai_standard", "openai");
        catalog.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.openai.com/v1/chat/completions\"}]");
        catalog.setPricing("[{\"providerModelId\":\"gpt-4o\",\"inputPrice\":2.5,\"outputPrice\":10.0}]");
        when(planCatalogGateway.findByPlanCode("openai_standard")).thenReturn(Optional.of(catalog));
        when(providerGateway.findByCode("openai")).thenReturn(Optional.empty());

        // Mock ObjectMapper 解析
        try {
            when(objectMapper.readValue(contains("protocol"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(java.util.List.of(java.util.Map.of("protocol", "OPENAI", "url", "https://api.openai.com/v1/chat/completions")));
            when(objectMapper.readValue(contains("inputPrice"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenReturn(java.util.List.of(java.util.Map.of("providerModelId", "gpt-4o", "inputPrice", 2.5, "outputPrice", 10.0)));
        } catch (Exception e) {
            // ignore
        }

        var result = service.getPlanDetail("openai_standard");

        assertNotNull(result);
        assertEquals("openai_standard", result.getPlanCode());
        assertEquals("openai", result.getProviderCode());
    }

    @Test
    void getPlanDetail_notFound_throwsException() {
        when(planCatalogGateway.findByPlanCode("unknown")).thenReturn(Optional.empty());

        assertThrows(com.codingas.gateway.domain.supply.catalog.exception.CatalogException.class,
                () -> service.getPlanDetail("unknown"));
    }

    @Test
    void listModelSpecCatalogs_returnsActiveCatalogs() {
        ModelSpecCatalog catalog = createModelSpecCatalog("gpt-4o", "GPT-4o");
        when(modelSpecCatalogGateway.findAll()).thenReturn(List.of(catalog));
        when(modelSpecGateway.findByProviderModelId("gpt-4o")).thenReturn(Optional.empty());
        // findProviderCodeForModel 会调用 planModelCatalogGateway
        when(planModelCatalogGateway.findByProviderModelId("gpt-4o")).thenReturn(List.of());

        var result = service.listModelSpecCatalogs(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("gpt-4o", result.get(0).getProviderModelId());
        assertFalse(result.get(0).getMaterialized());
    }

    // ===== 辅助方法 =====

    private ProviderCatalog createProviderCatalog(String code, String name) {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode(code);
        catalog.setProviderName(name);
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private PlanCatalog createPlanCatalog(String planCode, String providerCode) {
        PlanCatalog catalog = new PlanCatalog();
        catalog.setPlanCode(planCode);
        catalog.setProviderCode(providerCode);
        catalog.setPlanName("Standard Plan");
        catalog.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }

    private ModelSpecCatalog createModelSpecCatalog(String providerModelId, String displayName) {
        ModelSpecCatalog catalog = new ModelSpecCatalog();
        catalog.setProviderModelId(providerModelId);
        catalog.setDisplayName(displayName);
        catalog.setContextWindow(128000);
        catalog.setMaxOutputTokens(16384);
        catalog.setSource(CatalogSource.BUILTIN);
        catalog.setState(CatalogState.ACTIVE);
        return catalog;
    }
}