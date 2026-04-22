package com.codingas.gateway.adapter;

import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM 提供商适配器接口
 *
 * <p>所有 LLM 提供商 (OpenAI, Anthropic, 等) 必须实现此接口。</p>
 *
 * <p>设计原则:</p>
 * <ul>
 *   <li>使用响应式编程 (Reactor) 支持高并发</li>
 *   <li>流式响应使用 Flux 返回</li>
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
     * 发送非流式请求
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    Mono<LLMResponse> chat(LLMRequest request);

    /**
     * 发送流式请求
     *
     * @param request LLM 请求
     * @return 流式响应事件流
     */
    Flux<LLMResponse> chatStream(LLMRequest request);

    /**
     * 检查适配器是否可用
     *
     * @return true 如果适配器已配置并可用
     */
    boolean isAvailable();

    /**
     * 获取默认超时时间 (秒)
     *
     * @return 超时时间
     */
    default int getDefaultTimeout() {
        return 30;
    }
}
