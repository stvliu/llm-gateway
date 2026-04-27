package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.LLMProviderPort;
import com.codingas.gateway.domain.router.gateway.LLMProviderRegistry;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * DefaultModelRouter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultModelRouter")
class DefaultModelRouterTest {

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private LLMProviderRegistry providerRegistry;

    @Mock
    private LLMProviderPort providerPort;

    private DefaultModelRouter router;

    @BeforeEach
    void setUp() {
        router = new DefaultModelRouter(modelGateway, providerGateway, providerRegistry);
    }

    @Test
    @DisplayName("select 应返回 LLMProviderPort 而非 Infrastructure 类型")
    void select_returnsDomainPort() {
        Model model = new Model();
        model.setId(1L);
        model.setModelCode("openai/gpt-4o");
        model.setStatus(Model.ModelStatus.ACTIVE);
        model.setProviderId(1L);

        Provider provider = new Provider();
        provider.setId(1L);
        provider.setProviderCode("openai");
        provider.setProviderType(Provider.ProviderTypeEnum.OPENAI);

        when(modelGateway.findByModelCode("openai/gpt-4o")).thenReturn(Optional.of(model));
        when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
        when(providerRegistry.getAdapter(ProviderType.OPENAI)).thenReturn(Optional.of(providerPort));
        when(providerPort.isAvailable()).thenReturn(true);

        LLMProviderPort result = router.select("openai/gpt-4o", RouteGroup.RoutingStrategy.RANDOM);

        assertThat(result).isSameAs(providerPort);
    }

    @Test
    @DisplayName("select 模型不存在应抛出 NoSuchElementException")
    void select_modelNotFound_throwsException() {
        when(modelGateway.findByModelCode("unknown/model")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> router.select("unknown/model", RouteGroup.RoutingStrategy.RANDOM))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Model not found");
    }

    @Test
    @DisplayName("select 提供商不存在应抛出 NoSuchElementException")
    void select_providerNotFound_throwsException() {
        Model model = new Model();
        model.setId(1L);
        model.setModelCode("test/model");
        model.setProviderId(99L);

        when(modelGateway.findByModelCode("test/model")).thenReturn(Optional.of(model));
        when(providerGateway.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> router.select("test/model", RouteGroup.RoutingStrategy.RANDOM))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Provider not found");
    }
}
