package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.anthropic.AnthropicAdapter;
import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnthropicAdapter 能力报告测试
 *
 * <p>验证 Anthropic Claude 适配器正确报告其能力。</p>
 */
@DisplayName("AnthropicAdapter Capability Reporting Test")
class AnthropicAdapterTest {

    private final AnthropicAdapter adapter = new AnthropicAdapter(
            "https://api.anthropic.com",
            "test-api-key",
            "2023-06-01",
            30
    );

    @Test
    @DisplayName("getProviderCode should return anthropic")
    void getProviderCodeShouldReturnAnthropic() {
        assertEquals("anthropic", adapter.getProviderCode());
    }

    @Test
    @DisplayName("getProviderType should return ANTHROPIC")
    void getProviderTypeShouldReturnAnthropic() {
        assertEquals(ProviderType.ANTHROPIC, adapter.getProviderType());
    }

    @Test
    @DisplayName("getCapabilities should not support chat completion (OpenAI format)")
    void getCapabilitiesShouldNotSupportChatCompletion() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertFalse(capabilities.supportsChatCompletion(),
                "Anthropic should not support OpenAI chat format");
    }

    @Test
    @DisplayName("getCapabilities should support messages (Anthropic format)")
    void getCapabilitiesShouldSupportMessages() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsMessages(),
                "Anthropic should support messages format");
    }

    @Test
    @DisplayName("getCapabilities should support streaming")
    void getCapabilitiesShouldSupportStreaming() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsStreaming(),
                "Anthropic should support streaming");
    }

    @Test
    @DisplayName("getCapabilities should support function calling")
    void getCapabilitiesShouldSupportFunctionCalling() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsFunctionCalling(),
                "Anthropic should support function calling");
    }

    @Test
    @DisplayName("getCapabilities should not support embeddings")
    void getCapabilitiesShouldNotSupportEmbeddings() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertFalse(capabilities.supportsEmbeddings(),
                "Anthropic should not support embeddings");
    }

    @Test
    @DisplayName("getCapabilities should include Claude models")
    void getCapabilitiesShouldIncludeClaudeModels() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertNotNull(capabilities.supportedModels());
        assertFalse(capabilities.supportedModels().isEmpty());

        assertTrue(capabilities.supportedModels().contains("claude-opus-4-5"),
                "Should support claude-opus-4-5");
        assertTrue(capabilities.supportedModels().contains("claude-sonnet-4-6"),
                "Should support claude-sonnet-4-6");
    }

    @Test
    @DisplayName("isAvailable should return true when API key is set")
    void isAvailableShouldReturnTrueWhenApiKeySet() {
        assertTrue(adapter.isAvailable());
    }

    @Test
    @DisplayName("isAvailable should return false when API key is empty")
    void isAvailableShouldReturnFalseWhenApiKeyEmpty() {
        AnthropicAdapter emptyKeyAdapter = new AnthropicAdapter(
                "https://api.anthropic.com",
                "",
                "2023-06-01",
                30
        );
        assertFalse(emptyKeyAdapter.isAvailable());
    }

    @Test
    @DisplayName("isAvailable should return false when API key is null")
    void isAvailableShouldReturnFalseWhenApiKeyNull() {
        AnthropicAdapter nullKeyAdapter = new AnthropicAdapter(
                "https://api.anthropic.com",
                null,
                "2023-06-01",
                30
        );
        assertFalse(nullKeyAdapter.isAvailable());
    }

    @Test
    @DisplayName("isHealthy should return true when available")
    void isHealthyShouldReturnTrueWhenAvailable() {
        assertTrue(adapter.isHealthy());
    }

    @Test
    @DisplayName("getDefaultTimeout should return configured timeout")
    void getDefaultTimeoutShouldReturnConfiguredTimeout() {
        assertEquals(30, adapter.getDefaultTimeout());

        AnthropicAdapter customTimeoutAdapter = new AnthropicAdapter(
                "https://api.anthropic.com",
                "test-key",
                "2023-06-01",
                60
        );
        assertEquals(60, customTimeoutAdapter.getDefaultTimeout());
    }

    @Test
    @DisplayName("adapter providerType should be ANTHROPIC enum value")
    void adapterProviderTypeShouldBeAnthropicEnum() {
        assertSame(ProviderType.ANTHROPIC, adapter.getProviderType());
    }
}
