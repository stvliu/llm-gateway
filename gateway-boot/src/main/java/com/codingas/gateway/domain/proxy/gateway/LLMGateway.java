package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;

/**
 * LLM 网关接口
 *
 * <p>定义 LLM 代理的核心能力。</p>
 * <p>新架构下由 ProtocolGateway 体系实现，按协议而非供应商分发。</p>
 */
public interface LLMGateway {

    /**
     * 获取供应商名称
     */
    String getProviderName();

    /**
     * 非流式聊天
     *
     * @param request LLM 请求
     * @param baseUrl 端点 Base URL
     * @param apiKey API Key
     * @param timeoutSeconds 超时秒数
     * @return LLM 响应
     */
    LLMResponse chat(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds);

    /**
     * 流式聊天
     *
     * @param request LLM 请求
     * @param baseUrl 端点 Base URL
     * @param apiKey API Key
     * @param timeoutSeconds 超时秒数
     * @param callback 流式回调
     */
    void chatStream(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds, StreamCallback callback);
}