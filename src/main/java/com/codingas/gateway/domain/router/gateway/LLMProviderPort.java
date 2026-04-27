package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import reactor.core.publisher.Mono;

/**
 * LLM 提供商端口接口
 *
 * <p>Domain 层定义的接口，Infrastructure 层通过 Adapter 实现。</p>
 * <p>所有方法返回 Mono/Flux 类型以支持响应式编程。</p>
 */
public interface LLMProviderPort {

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
     * 发送非流式请求 (OpenAI 格式)
     *
     * @param request LLM 请求
     * @return LLM 响应 Mono
     */
    Mono<LLMResponse> chat(LLMRequest request);

    /**
     * 发送流式请求 (OpenAI 格式)
     *
     * @param request LLM 请求
     * @param callback 流式响应回调
     * @return 完成信号 Mono
     */
    Mono<Void> chatStream(LLMRequest request, StreamCallback callback);

    /**
     * 发送 Anthropic 消息 API 请求
     *
     * @param request LLM 请求
     * @return LLM 响应 Mono (Anthropic 格式)
     */
    Mono<LLMResponse> messages(LLMRequest request);

    /**
     * 发送 Anthropic 流式消息 API 请求
     *
     * @param request LLM 请求
     * @param callback 流式响应回调
     * @return 完成信号 Mono
     */
    Mono<Void> messagesStream(LLMRequest request, StreamCallback callback);

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
