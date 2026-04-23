package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.openai.OpenAIAdapter;
import com.codingas.gateway.adapter.anthropic.AnthropicAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdapterLoader SPI 发现机制测试
 *
 * <p>验证适配器实现正确实现 LLMProviderAdapter 接口。</p>
 */
@DisplayName("AdapterLoader SPI Discovery Test")
class AdapterLoaderTest {

    @Test
    @DisplayName("OpenAIAdapter and AnthropicAdapter should implement LLMProviderAdapter")
    void openaiAndAnthropicShouldImplementInterface() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertTrue(openai instanceof LLMProviderAdapter);
        assertTrue(anthropic instanceof LLMProviderAdapter);
    }

    @Test
    @DisplayName("adapters should have correct provider codes")
    void adaptersShouldHaveCorrectProviderCodes() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertEquals("openai", openai.getProviderCode());
        assertEquals("anthropic", anthropic.getProviderCode());
    }

    @Test
    @DisplayName("adapters should have correct provider types")
    void adaptersShouldHaveCorrectProviderTypes() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertEquals(ProviderType.OPENAI, openai.getProviderType());
        assertEquals(ProviderType.ANTHROPIC, anthropic.getProviderType());
    }

    @Test
    @DisplayName("loaded adapters should report capabilities")
    void adaptersShouldReportCapabilities() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertNotNull(openai.getCapabilities());
        assertNotNull(anthropic.getCapabilities());
        assertNotNull(openai.getCapabilities().supportedModels());
        assertNotNull(anthropic.getCapabilities().supportedModels());
    }

    @Test
    @DisplayName("OpenAI capabilities should be correct")
    void openaiCapabilitiesShouldBeCorrect() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );

        var caps = openai.getCapabilities();
        assertTrue(caps.supportsChatCompletion());
        assertFalse(caps.supportsMessages());
        assertTrue(caps.supportsStreaming());
        assertTrue(caps.supportedModels().contains("gpt-4o"));
    }

    @Test
    @DisplayName("Anthropic capabilities should be correct")
    void anthropicCapabilitiesShouldBeCorrect() {
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        var caps = anthropic.getCapabilities();
        assertFalse(caps.supportsChatCompletion());
        assertTrue(caps.supportsMessages());
        assertTrue(caps.supportsStreaming());
        assertTrue(caps.supportedModels().contains("claude-opus-4-5"));
    }

    @Test
    @DisplayName("adapters should be available when API key is set")
    void adaptersShouldBeAvailableWhenApiKeySet() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertTrue(openai.isAvailable());
        assertTrue(anthropic.isAvailable());
    }

    @Test
    @DisplayName("adapters should be healthy when available")
    void adaptersShouldBeHealthyWhenAvailable() {
        OpenAIAdapter openai = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                30
        );
        AnthropicAdapter anthropic = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                30
        );

        assertTrue(openai.isHealthy());
        assertTrue(anthropic.isHealthy());
    }
}
