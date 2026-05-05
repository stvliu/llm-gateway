package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AdapterLoader 单元测试
 */
@DisplayName("AdapterLoader 测试")
class AdapterLoaderTest {

    @Nested
    @DisplayName("loadAdapters 方法测试")
    class LoadAdaptersTests {

        @Test
        @DisplayName("加载适配器返回列表")
        void loadAdapters_returnsList() {
            // When
            List<LLMAdapter> adapters = AdapterLoader.loadAdapters();

            // Then
            assertThat(adapters).isNotNull();
            // 注意：SPI 加载的实际结果取决于 classpath 中的实现
        }

        @Test
        @DisplayName("多次调用返回新列表")
        void loadAdapters_multipleCalls_returnsIndependentLists() {
            // When
            List<LLMAdapter> adapters1 = AdapterLoader.loadAdapters();
            List<LLMAdapter> adapters2 = AdapterLoader.loadAdapters();

            // Then
            assertThat(adapters1).isNotNull();
            assertThat(adapters2).isNotNull();
            // 两个列表应该是独立的实例
            assertThat(adapters1).isNotSameAs(adapters2);
        }
    }
}
