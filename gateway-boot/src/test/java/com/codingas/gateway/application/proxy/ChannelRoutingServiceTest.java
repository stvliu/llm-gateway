package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.model.service.ApiKeySelectionService;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ChannelRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelRoutingServiceTest {

    @Mock
    private ModelDomainService modelDomainService;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ApiKeySelectionService apiKeySelectionService;

    @Mock
    private ProductRoutingService productRoutingService;

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    private ChannelRoutingService channelRoutingService;

    private Model createModel(Long id, Long providerId, Integer priority, Integer weight, BigDecimal inputPrice) {
        Model model = new Model();
        model.setId(id);
        model.setProviderId(providerId);
        model.setProviderModelId("gpt-4o");
        model.setDisplayName("GPT-4o");
        model.setPriority(priority);
        model.setWeight(weight);
        model.setInputPrice(inputPrice);
        model.setState(ModelState.ACTIVE);
        return model;
    }

    private Provider createProvider(Long id, String name) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(name);
        provider.setType(ProviderType.OPENAI);
        provider.setBaseUrl("https://api.openai.com");
        provider.setTimeout(30000);
        return provider;
    }

    private ProviderApiKey createApiKey(Long id, Long providerId) {
        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setId(id);
        apiKey.setProviderId(providerId);
        apiKey.setKeyName("test-key");
        apiKey.setApiKey("sk-test");
        apiKey.setIsDefault(true);
        return apiKey;
    }

    @BeforeEach
    void setUp() {
        channelRoutingService = new ChannelRoutingService(
            modelDomainService, providerGateway, apiKeySelectionService,
            productRoutingService, userApiKeyGateway);
    }

    @Nested
    @DisplayName("旧架构 resolve 方法测试")
    class LegacyResolveTests {

        @Test
        @DisplayName("modelName 为 null 时抛出 IllegalArgumentException")
        void resolve_nullModelName_throwsException() {
            assertThatThrownBy(() -> channelRoutingService.resolve(null, RouteGroup.RoutingStrategy.FAILOVER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model name is required");
        }

        @Test
        @DisplayName("modelName 为空字符串时抛出 IllegalArgumentException")
        void resolve_blankModelName_throwsException() {
            assertThatThrownBy(() -> channelRoutingService.resolve("  ", RouteGroup.RoutingStrategy.FAILOVER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model name is required");
        }

        @Test
        @DisplayName("多渠道按 FAILOVER 策略选择返回最高优先级")
        void resolve_multipleChannels_fairoverStrategy_returnsHighestPriority() {
            // Arrange
            Model model1 = createModel(1L, 100L, 10, 60, null);
            Model model2 = createModel(2L, 200L, 20, 40, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model1, model2));
            when(providerGateway.findById(100L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.FAILOVER);

            // Assert
            assertThat(ctx.getModel()).isEqualTo("gpt-4o");
            assertThat(ctx.getProviderId()).isEqualTo(100L);
            verify(apiKeySelectionService).selectApiKey(100L);
        }

        @Test
        @DisplayName("单渠道直接返回")
        void resolve_singleChannel_returnsDirectly() {
            // Arrange
            Model model = createModel(1L, 100L, 10, 100, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model));
            when(providerGateway.findById(100L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.FAILOVER);

            // Assert
            assertThat(ctx.getModel()).isEqualTo("gpt-4o");
        }

        @Test
        @DisplayName("无活跃渠道时 fallback 到旧逻辑")
        void resolve_noActiveChannels_fallbackToLegacy() {
            // Arrange
            Model model = createModel(1L, 100L, 10, 100, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of());
            when(modelDomainService.getModelWithProviderByProviderModelId("gpt-4o"))
                .thenReturn(new ModelDomainService.ModelProviderInfo(model, provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.FAILOVER);

            // Assert
            assertThat(ctx.getModel()).isEqualTo("gpt-4o");
            verify(modelDomainService).getModelWithProviderByProviderModelId("gpt-4o");
        }

        @Test
        @DisplayName("无可用 API Key 时抛出 IllegalStateException")
        void resolve_noAvailableApiKey_throwsException() {
            // Arrange
            Model model = createModel(1L, 100L, 10, 100, null);
            Provider provider = createProvider(100L, "OpenAI");

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model));
            when(providerGateway.findById(100L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.FAILOVER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No available API key");
        }

        @Test
        @DisplayName("Provider 不存在时抛出 NoSuchElementException")
        void resolve_providerNotFound_throwsException() {
            // Arrange
            Model model = createModel(1L, 100L, 10, 100, null);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model));
            when(providerGateway.findById(100L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.FAILOVER))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Provider not found");
        }
    }

    @Nested
    @DisplayName("COST_OPTIMIZED 策略测试")
    class CostOptimizedTests {

        @Test
        @DisplayName("选择价格最低的渠道")
        void resolve_costOptimized_selectsLowestPrice() {
            // Arrange
            Model model1 = createModel(1L, 100L, 10, 60, new BigDecimal("5.00"));
            Model model2 = createModel(2L, 200L, 20, 40, new BigDecimal("3.00"));
            Model model3 = createModel(3L, 300L, 30, 20, new BigDecimal("4.00"));
            Provider provider = createProvider(200L, "Azure");
            ProviderApiKey apiKey = createApiKey(1L, 200L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model1, model2, model3));
            when(providerGateway.findById(200L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(200L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.COST_OPTIMIZED);

            // Assert
            assertThat(ctx.getProviderId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("所有渠道无价格时返回第一个并记录警告")
        void resolve_costOptimized_allChannelsNoPrice_returnsFirst() {
            // Arrange
            Model model1 = createModel(1L, 100L, 10, 60, null);
            Model model2 = createModel(2L, 200L, 20, 40, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model1, model2));
            when(providerGateway.findById(100L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.COST_OPTIMIZED);

            // Assert
            assertThat(ctx.getProviderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("部分渠道无价格时忽略无价格渠道")
        void resolve_costOptimized_someChannelsNoPrice_ignoresNullPrice() {
            // Arrange
            Model model1 = createModel(1L, 100L, 10, 60, null);
            Model model2 = createModel(2L, 200L, 20, 40, new BigDecimal("3.00"));
            Provider provider = createProvider(200L, "Azure");
            ProviderApiKey apiKey = createApiKey(1L, 200L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model1, model2));
            when(providerGateway.findById(200L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(200L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.COST_OPTIMIZED);

            // Assert
            assertThat(ctx.getProviderId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("RANDOM 策略测试")
    class RandomTests {

        @Test
        @DisplayName("随机策略返回其中一个渠道")
        void resolve_random_returnsOneOfChannels() {
            // Arrange
            Model model1 = createModel(1L, 100L, 10, 50, null);
            Model model2 = createModel(2L, 200L, 20, 50, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model1, model2));
            when(providerGateway.findById(anyLong())).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(anyLong())).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve("gpt-4o", RouteGroup.RoutingStrategy.RANDOM);

            // Assert
            assertThat(ctx.getProviderId()).isIn(100L, 200L);
        }
    }

    @Nested
    @DisplayName("双路路由测试")
    class DualPathTests {

        @Test
        @DisplayName("新架构认证结果走 ProductRoutingService")
        void resolve_newArchitecture_usesProductRouting() {
            // Arrange
            UserAuthResult authResult = UserAuthResult.newArch(1L, "USER", 101L, 200L, 101L, 300L);
            RoutingContext expected = RoutingContext.builder()
                .productId(200L)
                .model("gpt-4o")
                .build();

            when(productRoutingService.resolve(any(), anyString(), anyString())).thenReturn(expected);
            when(userApiKeyGateway.findById(101L)).thenReturn(Optional.of(new com.codingas.gateway.domain.team.entity.UserApiKey()));

            // Act
            RoutingContext ctx = channelRoutingService.resolve(authResult, "gpt-4o", "openai", RouteGroup.RoutingStrategy.WEIGHTED);

            // Assert
            assertThat(ctx.getProductId()).isEqualTo(200L);
            verify(productRoutingService).resolve(any(), eq("gpt-4o"), eq("openai"));
        }

        @Test
        @DisplayName("新架构认证结果使用 anthropic 协议")
        void resolve_newArchitecture_anthropicProtocol_usesAnthropicProtocol() {
            // Arrange
            UserAuthResult authResult = UserAuthResult.newArch(1L, "USER", 101L, 200L, 101L, 300L);
            RoutingContext expected = RoutingContext.builder()
                .productId(200L)
                .model("claude-3-opus")
                .build();

            when(productRoutingService.resolve(any(), anyString(), anyString())).thenReturn(expected);
            when(userApiKeyGateway.findById(101L)).thenReturn(Optional.of(new com.codingas.gateway.domain.team.entity.UserApiKey()));

            // Act
            RoutingContext ctx = channelRoutingService.resolve(authResult, "claude-3-opus", "anthropic", RouteGroup.RoutingStrategy.WEIGHTED);

            // Assert
            assertThat(ctx.getProductId()).isEqualTo(200L);
            verify(productRoutingService).resolve(any(), eq("claude-3-opus"), eq("anthropic"));
        }

        @Test
        @DisplayName("旧架构认证结果走 legacy 路由")
        void resolve_legacyArchitecture_usesLegacyRouting() {
            // Arrange
            UserAuthResult authResult = UserAuthResult.legacy(1L, "USER", 10L);
            Model model = createModel(1L, 100L, 10, 100, null);
            Provider provider = createProvider(100L, "OpenAI");
            ProviderApiKey apiKey = createApiKey(1L, 100L);

            when(modelDomainService.findActiveChannels("gpt-4o")).thenReturn(List.of(model));
            when(providerGateway.findById(100L)).thenReturn(Optional.of(provider));
            when(apiKeySelectionService.selectApiKey(100L)).thenReturn(apiKey);

            // Act
            RoutingContext ctx = channelRoutingService.resolve(authResult, "gpt-4o", "openai", RouteGroup.RoutingStrategy.FAILOVER);

            // Assert
            assertThat(ctx.getProviderId()).isEqualTo(100L);
            verifyNoInteractions(productRoutingService);
        }
    }
}