package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.enums.ProductApiKeyState;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.enums.ProductType;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProductRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRoutingService 测试")
class ProductRoutingServiceTest {

    @Mock
    private ProductGateway productGateway;

    @Mock
    private ProductApiKeyGateway productApiKeyGateway;

    @Mock
    private ProviderGateway providerGateway;

    private ProductRoutingService service;

    @BeforeEach
    void setUp() {
        service = new ProductRoutingService(productGateway, productApiKeyGateway, providerGateway);
    }

    private Product createProduct(Long id, Long providerId, String name, ProductState state, List<String> models) {
        Product product = new Product();
        product.setId(id);
        product.setProviderId(providerId);
        product.setProviderName("TestProvider");
        product.setName(name);
        product.setState(state);
        product.setModels(models);
        product.setProductType(ProductType.PAY_AS_YOU_GO);
        product.setEndpoints(java.util.Map.of("openai", "https://api.openai.com", "anthropic", "https://api.anthropic.com"));
        return product;
    }

    private UserApiKey createUserApiKey(Long id, Long productId, Long teamId, List<String> models) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setId(id);
        userApiKey.setProductId(productId);
        userApiKey.setTeamId(teamId);
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
        provider.setType(ProviderType.OPENAI);
        return provider;
    }

    @Nested
    @DisplayName("resolve 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("成功解析路由 — 使用默认 ProductApiKey")
        void resolve_success_withDefaultApiKey() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o", "gpt-4"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            RoutingContext ctx = service.resolve(userApiKey, "gpt-4o", "openai");

            // then
            assertThat(ctx).isNotNull();
            assertThat(ctx.getProductId()).isEqualTo(100L);
            assertThat(ctx.getModel()).isEqualTo("gpt-4o");
            assertThat(ctx.getProtocol()).isEqualTo("openai");
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.openai.com");
            assertThat(ctx.getProviderApiKey()).isEqualTo("sk-provider-key-1");
            assertThat(ctx.isNewArchitecture()).isTrue();
        }

        @Test
        @DisplayName("成功解析路由 — 使用 anthropic 协议")
        void resolve_success_withAnthropicProtocol() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("claude-3-opus"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("claude-3-opus"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            RoutingContext ctx = service.resolve(userApiKey, "claude-3-opus", "anthropic");

            // then
            assertThat(ctx).isNotNull();
            assertThat(ctx.getProtocol()).isEqualTo("anthropic");
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.anthropic.com");
        }

        @Test
        @DisplayName("失败 — 产品不存在")
        void resolve_productNotFound_throwsException() {
            // given
            UserApiKey userApiKey = createUserApiKey(1L, 999L, 10L, List.of("gpt-4o"));
            when(productGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4o", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("失败 — 产品不可用")
        void resolve_productNotAvailable_throwsException() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.INACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            when(productGateway.findById(100L)).thenReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4o", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product is not available");
        }

        @Test
        @DisplayName("失败 — 产品不包含请求的模型")
        void resolve_modelNotInProduct_throwsException() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4", "gpt-4o"));
            when(productGateway.findById(100L)).thenReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4-turbo", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available in product");
        }

        @Test
        @DisplayName("失败 — UserApiKey 无权限访问模型")
        void resolve_userApiKeyNoPermission_throwsException() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o", "gpt-4-turbo"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            when(productGateway.findById(100L)).thenReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4-turbo", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not have permission to access model");
        }

        @Test
        @DisplayName("失败 — 无可用的 ProductApiKey")
        void resolve_noAvailableApiKey_throwsException() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.empty());
            when(productApiKeyGateway.findActiveByProductId(100L)).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4o", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No available ProductApiKey");
        }

        @Test
        @DisplayName("失败 — Provider 不存在")
        void resolve_providerNotFound_throwsException() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.resolve(userApiKey, "gpt-4o", "openai"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Provider not found");
        }
    }

    @Nested
    @DisplayName("ProductApiKey 选择策略测试")
    class ApiKeySelectionTests {

        @Test
        @DisplayName("默认 Key 可用时优先使用")
        void selectDefaultKey_whenAvailable() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            RoutingContext ctx = service.resolve(userApiKey, "gpt-4o", "openai");

            // then
            assertThat(ctx.getProviderApiKeyId()).isEqualTo(1L);
            verify(productApiKeyGateway, never()).findActiveByProductId(anyLong());
        }

        @Test
        @DisplayName("默认 Key 不可用时降级到活跃 Key 列表")
        void fallbackToActiveKeys_whenDefaultNotAvailable() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.INACTIVE);
            ProductApiKey activeKey = createProductApiKey(2L, 100L, 2, 50, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(productApiKeyGateway.findActiveByProductId(100L)).thenReturn(List.of(activeKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            RoutingContext ctx = service.resolve(userApiKey, "gpt-4o", "openai");

            // then
            assertThat(ctx.getProviderApiKeyId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("多个活跃 Key 时按权重随机选择")
        void selectByWeight_whenMultipleActiveKeys() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey key1 = createProductApiKey(1L, 100L, 1, 80, ProductApiKeyState.ACTIVE);
            ProductApiKey key2 = createProductApiKey(2L, 100L, 2, 20, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.empty());
            when(productApiKeyGateway.findActiveByProductId(100L)).thenReturn(List.of(key1, key2));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when - 多次调用验证权重选择
            int key1Count = 0;
            int iterations = 100;
            for (int i = 0; i < iterations; i++) {
                RoutingContext ctx = service.resolve(userApiKey, "gpt-4o", "openai");
                if (ctx.getProviderApiKeyId().equals(1L)) {
                    key1Count++;
                }
            }

            // then - key1 权重 80，key2 权重 20，期望 key1 被选中约 80%
            assertThat(key1Count).isGreaterThan(50); // 允许一定随机性
        }
    }

    @Nested
    @DisplayName("端点解析测试")
    class EndpointResolutionTests {

        @Test
        @DisplayName("使用请求协议对应的端点")
        void useProtocolEndpoint() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("claude-3-opus"));
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("claude-3-opus"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            RoutingContext ctx = service.resolve(userApiKey, "claude-3-opus", "anthropic");

            // then
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.anthropic.com");
        }

        @Test
        @DisplayName("协议不支持时使用默认端点")
        void useDefaultEndpoint_whenProtocolNotSupported() {
            // given
            Product product = createProduct(100L, 1L, "Test Product", ProductState.ACTIVE, List.of("gpt-4o"));
            product.setEndpoints(java.util.Map.of("openai", "https://api.openai.com")); // 只有 openai
            UserApiKey userApiKey = createUserApiKey(1L, 100L, 10L, List.of("gpt-4o"));
            ProductApiKey defaultKey = createProductApiKey(1L, 100L, 1, 100, ProductApiKeyState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(productGateway.findById(100L)).thenReturn(Optional.of(product));
            when(productApiKeyGateway.findDefaultByProductId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when - 请求 anthropic 但只有 openai 端点
            RoutingContext ctx = service.resolve(userApiKey, "gpt-4o", "anthropic");

            // then - 使用默认端点
            assertThat(ctx.getEndpoint()).isEqualTo("https://api.openai.com");
        }
    }
}