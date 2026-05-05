package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * AdapterRegistry 单元测试
 */
@DisplayName("AdapterRegistry 测试")
class AdapterRegistryTest {

    private AdapterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AdapterRegistry();
    }

    @Nested
    @DisplayName("注册适配器测试")
    class RegisterTests {

        @Test
        @DisplayName("注册单个适配器")
        void register_singleAdapter() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);

            // When
            registry.register(adapter);

            // Then
            assertThat(registry.hasAdapter("openai")).isTrue();
            assertThat(registry.hasAdapter(ProviderType.OPENAI)).isTrue();
        }

        @Test
        @DisplayName("批量注册适配器")
        void registerAll_multipleAdapters() {
            // Given
            LLMAdapter openaiAdapter = createMockAdapter("openai", ProviderType.OPENAI);
            LLMAdapter anthropicAdapter = createMockAdapter("anthropic", ProviderType.ANTHROPIC);

            // When
            registry.registerAll(List.of(openaiAdapter, anthropicAdapter));

            // Then
            assertThat(registry.getAllAdapters()).hasSize(2);
            assertThat(registry.hasAdapter("openai")).isTrue();
            assertThat(registry.hasAdapter("anthropic")).isTrue();
        }

        @Test
        @DisplayName("注册空列表")
        void registerAll_emptyList() {
            // When
            registry.registerAll(List.of());

            // Then
            assertThat(registry.getAllAdapters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("获取适配器测试")
    class GetAdapterTests {

        @Test
        @DisplayName("根据 providerCode 获取适配器")
        void getAdapter_byCode() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);
            registry.register(adapter);

            // When
            Optional<LLMAdapter> result = registry.getAdapter("openai");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(adapter);
        }

        @Test
        @DisplayName("根据不存在的 providerCode 获取返回空")
        void getAdapter_unknownCode_returnsEmpty() {
            // When
            Optional<LLMAdapter> result = registry.getAdapter("unknown");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("获取所有适配器返回不可变列表")
        void getAllAdapters_returnsImmutableList() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);
            registry.register(adapter);

            // When
            List<LLMAdapter> adapters = registry.getAllAdapters();

            // Then
            assertThat(adapters).hasSize(1);
            assertThatThrownBy(() -> adapters.add(adapter))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("检查适配器存在测试")
    class HasAdapterTests {

        @Test
        @DisplayName("hasAdapter ProviderType 存在返回 true")
        void hasAdapter_typeExists_returnsTrue() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);
            registry.register(adapter);

            // When & Then
            assertThat(registry.hasAdapter(ProviderType.OPENAI)).isTrue();
        }

        @Test
        @DisplayName("hasAdapter ProviderType 不存在返回 false")
        void hasAdapter_typeNotExists_returnsFalse() {
            // When & Then
            assertThat(registry.hasAdapter(ProviderType.OPENAI)).isFalse();
        }

        @Test
        @DisplayName("hasAdapter code 存在返回 true")
        void hasAdapter_codeExists_returnsTrue() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);
            registry.register(adapter);

            // When & Then
            assertThat(registry.hasAdapter("openai")).isTrue();
        }

        @Test
        @DisplayName("hasAdapter code 不存在返回 false")
        void hasAdapter_codeNotExists_returnsFalse() {
            // When & Then
            assertThat(registry.hasAdapter("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("LLMGatewayRegistry 接口测试")
    class LLMGatewayRegistryTests {

        @Test
        @DisplayName("getGateway 返回 LLMGateway")
        void getGateway_returnsGateway() {
            // Given
            LLMAdapter adapter = createMockAdapter("openai", ProviderType.OPENAI);
            registry.register(adapter);

            // When
            Optional<LLMGateway> result = registry.getGateway(ProviderType.OPENAI);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(LLMGateway.class);
        }

        @Test
        @DisplayName("getGateway 不存在返回空")
        void getGateway_notFound_returnsEmpty() {
            // When
            Optional<LLMGateway> result = registry.getGateway(ProviderType.OPENAI);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getAllGateways 返回所有网关")
        void getAllGateways_returnsAll() {
            // Given
            LLMAdapter openaiAdapter = createMockAdapter("openai", ProviderType.OPENAI);
            LLMAdapter anthropicAdapter = createMockAdapter("anthropic", ProviderType.ANTHROPIC);
            registry.registerAll(List.of(openaiAdapter, anthropicAdapter));

            // When
            Iterable<LLMGateway> gateways = registry.getAllGateways();

            // Then
            assertThat(gateways).hasSize(2);
        }
    }

    // Helper method
    private LLMAdapter createMockAdapter(String code, ProviderType type) {
        LLMAdapter adapter = mock(LLMAdapter.class);
        when(adapter.getProviderCode()).thenReturn(code);
        when(adapter.getProviderType()).thenReturn(type);
        return adapter;
    }
}
