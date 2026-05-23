package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;

/**
 * 协议网关接口
 *
 * <p>按协议（而非供应商）定义请求/响应处理能力。</p>
 * <p>每个协议类型对应一个实现类，实现类通过 getProtocolName() 自声明唯一标识。</p>
 *
 * <p><b>架构说明：</b>此接口依赖 Application 层 DTO 是已知的技术债务。
 * 完整修复需要在 Domain 层定义独立的值对象，但这需要大量重构工作。
 * 参考：domain/proxy/valueobject/LLMRequestVO.java</p>
 */
public interface ProtocolGateway {

    /**
     * 协议唯一标识（如 "openai", "anthropic"）
     */
    String getProtocolName();

    /**
     * 协议显示名称
     */
    String getProtocolLabel();

    /**
     * 验证 API Key 格式是否合法
     */
    boolean validateApiKeyFormat(String apiKey);

    /**
     * 非流式聊天请求
     */
    LLMResponse chat(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds);

    /**
     * 流式聊天请求
     */
    void chatStream(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds, StreamCallback callback);

    /**
     * 获取默认 Base URL
     */
    String getDefaultBaseUrl();

    /**
     * 获取默认测试模型
     */
    String getDefaultTestModel();

    /**
     * 连通性测试
     */
    ConnectivityTestResult testConnectivity(String apiKey, String baseUrl, String model);
}
