package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.product.service.ProductDomainService;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.service.UserApiKeyDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProductRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRoutingService 测试")
class ProductRoutingServiceTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private ProductGateway productGateway;

    @Mock
    private ProductApiKeyGateway productApiKeyGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ProductDomainService productDomainService;

    private UserApiKeyDomainService userApiKeyDomainService;
    private ProductRoutingService service;

    @BeforeEach
    void setUp() {
        userApiKeyDomainService = new UserApiKeyDomainService();
        service = new ProductRoutingService(
                userApiKeyGateway, productGateway, productApiKeyGateway, providerGateway,
                userApiKeyDomainService, productDomainService);
    }

    private Product createProduct(Long id, Long providerId, String name, ProductState state) {
        Product product = new Product();
        product.setId(id);
        product.setProviderId(providerId);
        product.setProviderName("TestProvider");
        product.setName(name);
        product.setState(state);
        product.setProductType(ProductType.PAY_AS_YOU_GO);
        product.setEndpoints(java.util.Map.of("openai", "https://api.openai.com", "anthropic", "https://api.anthropic.com"));
        return product;
    }

    private UserApiKey createUserApiKey(Long id, List<Long> productIds, List<String> models) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setId(id);
        userApiKey.setProductIds(productIds);
        userApiKey.setState(UserApiKeyState.ACTIVE);
        userApiKey.setModels(models);
        return userApiKey;
    }

    private ProductApiKey createProductApiKey(Long id, Long productId, Integer priority, Integer weight, ProductApiKeyState state) {
        ProductApiKey apiKey = new ProductApiKey();
        apiKey.setId(id);
        apiKey.setProductId(productId);
        apiKey.setPriority(priority);
        apiKey.setWeight(weight);
        apiKey.setState(state);
        apiKey.setApiKeyPlain("sk-provider-key-" + id);
        return apiKey;
    }

    private Provider createProvider(Long id, String name) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(name);
        return provider;
    }

    @Nested
    @DisplayName("resolve 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("成功解析路由 — 使用默认 ProductApiKey")
        void resolve_success_withDefaultApiKey() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx).isNotNull();
            assertThat(ctx.getProductId()).isEqualTo(100L);
            assertThat(ctx.getModel()).isEqualTo("gpt-4o");
            assertThat(ctx.getProtocol()).isEqualTo("openai");
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.openai.com");
            assertThat(ctx.getProviderApiKey()).isEqualTo("sk-provider-key-1");
        }

        @Test
        @DisplayName("成功解析路由 — 使用 anthropic 协议")
        void resolve_success_withAnthropicProtocol() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("claude-3-opus"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "claude-3-opus")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "claude-3-opus", "anthropic");

            assertThat(ctx).isNotNull();
            assertThat(ctx.getProtocol()).isEqualTo("anthropic");
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.anthropic.com");
        }

        @Test
        @DisplayName("失败 — UserApiKey 不存在")
        void resolve_userApiKeyNotFound_throwsException() {
            when(userApiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(999L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("UserApiKey");
        }

        @Test
        @DisplayName("失败 — 无可用的 ProductApiKey")
        void resolve_noAvailableApiKey_throwsException() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.empty());
            when(productApiKeyGateway.findActiveByProductId(100L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.resolve(1L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("ProductApiKey");
        }

        @Test
        @DisplayName("失败 — Provider 不存在")
        void resolve_providerNotFound_throwsException() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(1L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Provider");
        }
    }

    @Nested
    @DisplayName("ProductApiKey 选择策略测试")
    class ApiKeySelectionTests {

        @Test
        @DisplayName("默认 Key 可用时优先使用")
        void selectDefaultKey_whenAvailable() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.getProviderApiKeyId()).isEqualTo(1L);
            verify(productApiKeyGateway, never()).findActiveByProductId(anyLong());
        }

        @Test
        @DisplayName("默认 Key 不可用时降级到活跃 Key 列表")
        void fallbackToActiveKeys_whenDefaultNotAvailable() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.INACTIVE);
            ProductApiKey activeKey = createProductApiKey(2L, 100L, 2, 50, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(productApiKeyGateway.findActiveByProductId(100L)).thenReturn(List.of(activeKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.getProviderApiKeyId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("端点解析测试")
    class EndpointResolutionTests {

        @Test
        @DisplayName("使用请求协议对应的端点")
        void useProtocolEndpoint() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("claude-3-opus"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "claude-3-opus")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "claude-3-opus", "anthropic");

            assertThat(ctx.getEndpoint()).isEqualTo("https://api.anthropic.com");
        }

        @Test
        @DisplayName("协议不支持时使用默认端点")
        void useDefaultEndpoint_whenProtocolNotSupported() {
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE);
            product.setEndpoints(java.util.Map.of("openai", "https://api.openai.com"));
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(productGateway.findByIds(List.of(100L))).thenReturn(List.of(product));
            when(productDomainService.containsModel(product, "gpt-4o")).thenReturn(true);
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "anthropic");

            assertThat(ctx.getEndpoint()).isEqualTo("https://api.openai.com");
        }
    }
}