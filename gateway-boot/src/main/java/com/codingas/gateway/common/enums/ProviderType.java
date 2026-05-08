package com.codingas.gateway.common.enums;

/**
 * LLM 提供商类型枚举
 *
 * <p>定义了支持的 LLM 提供商类型。</p>
 * <p>按协议兼容性分为三类：</p>
 * <ul>
 *   <li>OpenAI 兼容：OPENAI, DEEPSEEK, MOONSHOT, ZHIPU, BAICHUAN, MINIMAX, VOLCENGINE</li>
 *   <li>Anthropic 协议：ANTHROPIC</li>
 *   <li>自定义协议：GEMINI, QWEN, WENXIN, TENCENT, XUNFEI</li>
 * </ul>
 */
public enum ProviderType {
    // ========== 国际主流厂商 ==========

    /** OpenAI（Chat Completions API 标准） */
    OPENAI,

    /** Anthropic（Messages API 标准） */
    ANTHROPIC,

    /** Google Gemini */
    GEMINI,

    // ========== 国内 OpenAI 兼容厂商 ==========

    /** DeepSeek（深度求索） */
    DEEPSEEK,

    /** Moonshot（月之暗面 Kimi） */
    MOONSHOT,

    /** 智谱 GLM */
    ZHIPU,

    /** 百川智能 */
    BAICHUAN,

    /** MiniMax */
    MINIMAX,

    /** 火山引擎（字节跳动） */
    VOLCENGINE,

    // ========== 国内自定义协议厂商 ==========

    /** 通义千问（阿里云） */
    QWEN,

    /** 文心一言（百度） */
    WENXIN,

    /** 腾讯混元 */
    TENCENT,

    /** 讯飞星火 */
    XUNFEI,

    // ========== 其他 ==========

    /** 其他类型 */
    OTHER
}
