package com.codingas.gateway.domain.supply.enums;

import lombok.Getter;

/**
 * 协议类型枚举
 *
 * <p>定义 API 端点支持的协议类型。</p>
 */
@Getter
public enum Protocol {

    /** OpenAI 原生/兼容协议 */
    OPENAI("openai", "OpenAI"),

    /** Anthropic Messages API */
    ANTHROPIC("anthropic", "Anthropic"),

    /** Google Gemini API */
    GEMINI("gemini", "Gemini"),

    /** 原生私有协议 */
    NATIVE("native", "Native");

    private final String code;
    private final String displayName;

    Protocol(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static Protocol fromCode(String code) {
        for (Protocol protocol : values()) {
            if (protocol.code.equalsIgnoreCase(code)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown protocol: " + code);
    }
}