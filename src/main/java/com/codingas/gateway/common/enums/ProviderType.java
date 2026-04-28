package com.codingas.gateway.common.enums;

/**
 * LLM 提供商类型枚举
 *
 * <p>定义了支持的 LLM 提供商类型。</p>
 */
public enum ProviderType {
    /** OpenAI */
    OPENAI,

    /** Anthropic */
    ANTHROPIC,

    /** Google Gemini */
    GEMINI,

    /** 智谱 GLM */
    ZHIPU,

    /** 通义千问 */
    QWEN,

    /** 火山引擎 */
    VOLCENGINE,

    /** 文心一言 */
    WENXIN,

    /** 其他类型 */
    OTHER
}
