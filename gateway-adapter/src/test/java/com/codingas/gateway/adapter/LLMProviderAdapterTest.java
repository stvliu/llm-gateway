package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMProviderAdapter 接口契约测试
 *
 * <p>验证所有必须的方法存在且签名正确。</p>
 */
@DisplayName("LLMProviderAdapter Interface Contract Test")
class LLMProviderAdapterTest {

    @Test
    @DisplayName("LLMProviderAdapter should have getProviderCode method returning String")
    void shouldHaveGetProviderCodeMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("getProviderCode");
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have getProviderType method returning ProviderType")
    void shouldHaveGetProviderTypeMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("getProviderType");
        assertEquals(ProviderType.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have chat method")
    void shouldHaveChatMethod() throws NoSuchMethodException {
        // chat 方法签名: LLMResponse chat(LLMRequest request)
        Method method = LLMProviderAdapter.class.getMethod("chat", LLMRequest.class);
        assertEquals(LLMResponse.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have chatStream method with StreamCallback")
    void shouldHaveChatStreamMethod() throws NoSuchMethodException {
        // chatStream 方法签名: void chatStream(LLMRequest request, StreamCallback callback)
        Method method = LLMProviderAdapter.class.getMethod("chatStream", LLMRequest.class, StreamCallback.class);
        assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have messages method")
    void shouldHaveMessagesMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("messages", LLMRequest.class);
        assertEquals(LLMResponse.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have messagesStream method with StreamCallback")
    void shouldHaveMessagesStreamMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("messagesStream", LLMRequest.class, StreamCallback.class);
        assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have isAvailable method returning boolean")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("isAvailable");
        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have isHealthy method returning boolean")
    void shouldHaveIsHealthyMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("isHealthy");
        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have checkConnection method returning boolean")
    void shouldHaveCheckConnectionMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("checkConnection");
        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have getCapabilities method returning ProviderCapabilities")
    void shouldHaveGetCapabilitiesMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("getCapabilities");
        assertEquals(ProviderCapabilities.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should have getDefaultTimeout method returning int")
    void shouldHaveGetDefaultTimeoutMethod() throws NoSuchMethodException {
        Method method = LLMProviderAdapter.class.getMethod("getDefaultTimeout");
        assertEquals(int.class, method.getReturnType());
    }

    @Test
    @DisplayName("LLMProviderAdapter should be an interface")
    void shouldBeInterface() {
        assertTrue(LLMProviderAdapter.class.isInterface());
    }
}
