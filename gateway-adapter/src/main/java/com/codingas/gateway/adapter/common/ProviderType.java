package com.codingas.gateway.adapter.common;

/**
 * LLM 提供商类型枚举
 *
 * <p>定义了支持的 LLM 提供商类型，与 {@link com.codingas.gateway.adapter.LLMProviderAdapter#getProviderType()} 对应。</p>
 */
public enum ProviderType {
    /** OpenAI 格式 */
    OPENAI,

    /** Anthropic 格式 */
    ANTHROPIC,

    /** Google Gemini */
    GEMINI,

    /** 智谱 GLM */
    ZHIPU,

    /** 其他类型 */
    OTHER
}
