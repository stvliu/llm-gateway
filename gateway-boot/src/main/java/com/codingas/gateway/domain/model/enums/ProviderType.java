package com.codingas.gateway.domain.model.enums;

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
    OPENAI("OpenAI"),

    /** Anthropic（Messages API 标准） */
    ANTHROPIC("Anthropic"),

    /** Google Gemini */
    GEMINI("Google Gemini"),

    // ========== 国内 OpenAI 兼容厂商 ==========

    /** DeepSeek（深度求索） */
    DEEPSEEK("DeepSeek"),

    /** Moonshot（月之暗面 Kimi） */
    MOONSHOT("Moonshot"),

    /** 智谱 GLM */
    ZHIPU("智谱 GLM"),

    /** 百川智能 */
    BAICHUAN("百川智能"),

    /** MiniMax */
    MINIMAX("MiniMax"),

    /** 火山引擎（字节跳动） */
    VOLCENGINE("火山引擎"),

    // ========== 国内自定义协议厂商 ==========

    /** 通义千问（阿里云） */
    QWEN("通义千问"),

    /** 文心一言（百度） */
    WENXIN("文心一言"),

    /** 腾讯混元 */
    TENCENT("腾讯混元"),

    /** 讯飞星火 */
    XUNFEI("讯飞星火"),

    // ========== 其他 ==========

    /** 其他类型 */
    OTHER("其他");

    private final String label;

    ProviderType(String label) {
        this.label = label;
    }

    /**
     * 获取供应商类型的显示名称
     *
     * @return 显示名称
     */
    public String getLabel() {
        return label;
    }
}
