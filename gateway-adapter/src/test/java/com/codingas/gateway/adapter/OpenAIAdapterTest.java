package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.openai.OpenAIAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAIAdapter 能力报告测试
 *
 * <p>验证 OpenAI 适配器正确报告其能力。</p>
 */
@DisplayName("OpenAIAdapter Capability Reporting Test")
class OpenAIAdapterTest {

    private final OpenAIAdapter adapter = new OpenAIAdapter(
            "https://api.openai.com",
            "test-api-key",
            30
    );

    @Test
    @DisplayName("getProviderCode should return openai")
    void getProviderCodeShouldReturnOpenai() {
        assertEquals("openai", adapter.getProviderCode());
    }

    @Test
    @DisplayName("getProviderType should return OPENAI")
    void getProviderTypeShouldReturnOpenai() {
        assertEquals(ProviderType.OPENAI, adapter.getProviderType());
    }

    @Test
    @DisplayName("getCapabilities should support chat completion")
    void getCapabilitiesShouldSupportChatCompletion() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsChatCompletion(),
                "OpenAI should support chat completion (OpenAI format)");
    }

    @Test
    @DisplayName("getCapabilities should not support messages (Anthropic format)")
    void getCapabilitiesShouldNotSupportMessages() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertFalse(capabilities.supportsMessages(),
                "OpenAI should not support Anthropic messages format");
    }

    @Test
    @DisplayName("getCapabilities should support streaming")
    void getCapabilitiesShouldSupportStreaming() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsStreaming(),
                "OpenAI should support streaming");
    }

    @Test
    @DisplayName("getCapabilities should support function calling")
    void getCapabilitiesShouldSupportFunctionCalling() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertTrue(capabilities.supportsFunctionCalling(),
                "OpenAI should support function calling");
    }

    @Test
    @DisplayName("getCapabilities should include GPT models")
    void getCapabilitiesShouldIncludeGptModels() {
        ProviderCapabilities capabilities = adapter.getCapabilities();
        assertNotNull(capabilities.supportedModels());
        assertFalse(capabilities.supportedModels().isEmpty());

        assertTrue(capabilities.supportedModels().contains("gpt-4o"),
                "Should support gpt-4o");
        assertTrue(capabilities.supportedModels().contains("gpt-4o-mini"),
                "Should support gpt-4o-mini");
    }

    @Test
    @DisplayName("isAvailable should return true when API key is set")
    void isAvailableShouldReturnTrueWhenApiKeySet() {
        assertTrue(adapter.isAvailable());
    }

    @Test
    @DisplayName("isAvailable should return false when API key is empty")
    void isAvailableShouldReturnFalseWhenApiKeyEmpty() {
        OpenAIAdapter emptyKeyAdapter = new OpenAIAdapter(
                "https://api.openai.com",
                "",
                30
        );
        assertFalse(emptyKeyAdapter.isAvailable());
    }

    @Test
    @DisplayName("isAvailable should return false when API key is null")
    void isAvailableShouldReturnFalseWhenApiKeyNull() {
        OpenAIAdapter nullKeyAdapter = new OpenAIAdapter(
                "https://api.openai.com",
                null,
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

        OpenAIAdapter customTimeoutAdapter = new OpenAIAdapter(
                "https://api.openai.com",
                "test-key",
                60
        );
        assertEquals(60, customTimeoutAdapter.getDefaultTimeout());
    }

    @Test
    @DisplayName("adapter providerType should be OPENAI enum value")
    void adapterProviderTypeShouldBeOpenaiEnum() {
        assertSame(ProviderType.OPENAI, adapter.getProviderType());
    }
}
