/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.enums;

/**
 * 协议类型枚举
 *
 * <p>定义 API 端点支持的协议类型。</p>
 */
public enum Protocol {

    /** OpenAI 原生/兼容协议 */
    OPENAI,

    /** Anthropic Messages API */
    ANTHROPIC,

    /** Google Gemini API */
    GEMINI,

    /** 原生私有协议 */
    NATIVE;

    /**
     * 根据代码获取协议枚举
     *
     * @param code 协议代码（不区分大小写）
     * @return 对应的协议枚举
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static Protocol fromCode(String code) {
        for (Protocol protocol : values()) {
            if (protocol.name().equalsIgnoreCase(code)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Unknown protocol: " + code);
    }
}
