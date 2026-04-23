package com.codingas.gateway.core.domain.enums;

/**
 * 模型提供商类型枚举
 *
 * <p>定义了支持的 LLM 提供商类型。</p>
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
