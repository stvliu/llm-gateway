package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;

/**
 * LLM 适配器接口
 *
 * <p>旧架构适配器接口，用于按供应商名称直接路由。</p>
 * <p>新架构下由 ProtocolGateway 体系替代，此接口仅用于过渡期兼容。</p>
 */
public interface LLMAdapter {

    /**
     * 获取供应商编码（与 getProviderName 相同）
     */
    String getProviderCode();

    /**
     * 获取供应商名称
     */
    String getProviderName();

    /**
     * 检查适配器是否可用
     */
    boolean isAvailable();

    /**
     * 健康检查
     */
    boolean isHealthy();

    /**
     * 执行连接检查
     */
    boolean checkConnection();

    /**
     * 测试连通性（支持分层结果）
     */
    ConnectivityTestResult testConnectivity(String apiKey, String baseUrl, String model);

    /**
     * 获取此供应商的默认测试模型
     */
    String getDefaultTestModel();

    /**
     * 是否需要用户提供测试模型
     */
    default boolean requiresUserProvidedModel() {
        return false;
    }

    /**
     * 获取此供应商的默认 Base URL
     */
    String getDefaultBaseUrl();

    /**
     * 非流式聊天（旧架构兼容）
     */
    LLMResponse chat(LLMRequest request);

    /**
     * 流式聊天（旧架构兼容）
     */
    void chatStream(LLMRequest request, StreamCallback callback);

    /**
     * Anthropic 消息 API（旧架构兼容）
     */
    default LLMResponse messages(LLMRequest request) {
        throw new UnsupportedOperationException("不支持 Anthropic Messages 格式");
    }

    /**
     * Anthropic 流式消息 API（旧架构兼容）
     */
    default void messagesStream(LLMRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException("不支持 Anthropic Messages 格式");
    }

    /**
     * 获取供应商能力
     */
    default com.codingas.gateway.domain.model.entity.ProviderCapabilities getCapabilities() {
        return null;
    }

    /**
     * 获取默认超时时间
     */
    int getDefaultTimeout();
}