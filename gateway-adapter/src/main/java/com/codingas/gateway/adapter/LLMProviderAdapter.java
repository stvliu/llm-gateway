package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;

/**
 * LLM 提供商适配器接口
 *
 * <p>所有 LLM 提供商 (OpenAI, Anthropic, 等) 必须实现此接口。</p>
 *
 * <p>设计原则:</p>
 * <ul>
 *   <li>使用 Spring MVC + RestClient (同步) 或 OkHttp (流式) 进行 HTTP 调用</li>
 *   <li>流式响应使用 StreamCallback 回调</li>
 *   <li>协议转换在适配器层完成</li>
 * </ul>
 *
 * @see <a href="https://docs.llm-gateway.dev/adapter">适配器开发文档</a>
 */
public interface LLMProviderAdapter {

    /**
     * 获取提供商编码
     *
     * @return 提供商唯一编码 (如 "openai", "anthropic")
     */
    String getProviderCode();

    /**
     * 获取提供商类型
     *
     * @return 提供商类型枚举
     */
    ProviderType getProviderType();

    /**
     * 发送非流式请求 (使用 RestClient)
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    LLMResponse chat(LLMRequest request);

    /**
     * 发送流式请求 (使用 OkHttp)
     *
     * @param request LLM 请求
     * @param callback 流式响应回调
     */
    void chatStream(LLMRequest request, StreamCallback callback);

    /**
     * 发送 Anthropic 消息 API 请求 (使用 RestClient)
     *
     * @param request LLM 请求
     * @return LLM 响应 (Anthropic 格式)
     */
    LLMResponse messages(LLMRequest request);

    /**
     * 发送 Anthropic 流式请求 (使用 OkHttp)
     *
     * @param request LLM 请求
     * @param callback 流式响应回调
     */
    void messagesStream(LLMRequest request, StreamCallback callback);

    /**
     * 检查适配器是否可用
     *
     * @return true 如果适配器已配置并可用
     */
    boolean isAvailable();

    /**
     * 健康检查
     *
     * @return true 表示 Provider 可用
     */
    boolean isHealthy();

    /**
     * 执行连接检查
     *
     * <p>实际验证与提供商的连接是否正常，用于故障转移判断。</p>
     *
     * @return true 如果连接正常
     */
    boolean checkConnection();

    /**
     * 获取 Provider 能力描述
     *
     * @return ProviderCapabilities
     */
    ProviderCapabilities getCapabilities();

    /**
     * 获取默认超时时间 (秒)
     *
     * @return 超时时间
     */
    default int getDefaultTimeout() {
        return 30;
    }
}
