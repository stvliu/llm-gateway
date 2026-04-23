package com.codingas.gateway.adapter.common;

import java.util.Set;

/**
 * 提供商能力描述
 *
 * <p>描述某个 LLM 提供商支持的功能和能力。</p>
 */
public record ProviderCapabilities(
    /** 提供商类型 */
    ProviderType providerType,

    /** 是否支持 OpenAI 格式聊天补全 */
    boolean supportsChatCompletion,

    /** 是否支持 Anthropic 格式消息 API */
    boolean supportsMessages,

    /** 是否支持向量嵌入 */
    boolean supportsEmbeddings,

    /** 是否支持流式响应 */
    boolean supportsStreaming,

    /** 是否支持函数调用 */
    boolean supportsFunctionCalling,

    /** 支持的模型 ID 列表 */
    Set<String> supportedModels
) {}
