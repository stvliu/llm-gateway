package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

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
            LLMAdapter adapter = createMockAdapter("openai", "openai");

            registry.register(adapter);

            assertThat(registry.hasAdapterByCode("openai")).isTrue();
            assertThat(registry.hasAdapterByName("openai")).isTrue();
        }

        @Test
        @DisplayName("批量注册适配器")
        void registerAll_multipleAdapters() {
            LLMAdapter openaiAdapter = createMockAdapter("openai", "openai");
            LLMAdapter anthropicAdapter = createMockAdapter("anthropic", "anthropic");

            registry.registerAll(List.of(openaiAdapter, anthropicAdapter));

            assertThat(registry.getAllAdapters()).hasSize(2);
            assertThat(registry.hasAdapterByCode("openai")).isTrue();
            assertThat(registry.hasAdapterByCode("anthropic")).isTrue();
        }

        @Test
        @DisplayName("注册空列表")
        void registerAll_emptyList() {
            registry.registerAll(List.of());

            assertThat(registry.getAllAdapters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("获取适配器测试")
    class GetAdapterTests {

        @Test
        @DisplayName("根据 providerCode 获取适配器")
        void getAdapter_byCode() {
            LLMAdapter adapter = createMockAdapter("openai", "openai");
            registry.register(adapter);

            Optional<LLMAdapter> result = registry.getAdapter("openai");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(adapter);
        }

        @Test
        @DisplayName("根据 providerName 获取适配器")
        void getAdapterByName_byName() {
            LLMAdapter adapter = createMockAdapter("openai", "openai");
            registry.register(adapter);

            Optional<LLMAdapter> result = registry.getAdapterByName("openai");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(adapter);
        }

        @Test
        @DisplayName("根据不存在的 providerCode 获取返回空")
        void getAdapter_unknownCode_returnsEmpty() {
            Optional<LLMAdapter> result = registry.getAdapter("unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("获取所有适配器返回不可变列表")
        void getAllAdapters_returnsImmutableList() {
            LLMAdapter adapter = createMockAdapter("openai", "openai");
            registry.register(adapter);

            List<LLMAdapter> adapters = registry.getAllAdapters();

            assertThat(adapters).hasSize(1);
            assertThatThrownBy(() -> adapters.add(adapter))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("检查适配器存在测试")
    class HasAdapterTests {

        @Test
        @DisplayName("hasAdapterByCode 存在返回 true")
        void hasAdapterByCode_exists_returnsTrue() {
            LLMAdapter adapter = createMockAdapter("openai", "openai");
            registry.register(adapter);

            assertThat(registry.hasAdapterByCode("openai")).isTrue();
        }

        @Test
        @DisplayName("hasAdapterByCode 不存在返回 false")
        void hasAdapterByCode_notExists_returnsFalse() {
            assertThat(registry.hasAdapterByCode("openai")).isFalse();
        }

        @Test
        @DisplayName("hasAdapterByName 存在返回 true")
        void hasAdapterByName_exists_returnsTrue() {
            LLMAdapter adapter = createMockAdapter("openai", "openai");
            registry.register(adapter);

            assertThat(registry.hasAdapterByName("openai")).isTrue();
        }

        @Test
        @DisplayName("hasAdapterByName 不存在返回 false")
        void hasAdapterByName_notExists_returnsFalse() {
            assertThat(registry.hasAdapterByName("unknown")).isFalse();
        }
    }

    private LLMAdapter createMockAdapter(String code, String name) {
        LLMAdapter adapter = mock(LLMAdapter.class);
        when(adapter.getProviderCode()).thenReturn(code);
        when(adapter.getProviderName()).thenReturn(name);
        return adapter;
    }
}
