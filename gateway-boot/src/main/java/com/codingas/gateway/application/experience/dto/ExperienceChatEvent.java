/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.experience.dto;

/**
 * 模型体验 SSE 事件
 */
public record ExperienceChatEvent(
    EventType type,
    Object data
) {

    public enum EventType {
        /** 内容增量 */
        CONTENT,
        /** Token 使用统计 */
        USAGE,
        /** 错误 */
        ERROR,
        /** 完成 */
        DONE
    }

    /**
     * 创建内容事件
     */
    public static ExperienceChatEvent content(String content) {
        return new ExperienceChatEvent(EventType.CONTENT, new ContentData(content));
    }

    /**
     * 创建使用量事件
     */
    public static ExperienceChatEvent usage(int promptTokens, int completionTokens) {
        return new ExperienceChatEvent(EventType.USAGE,
            new UsageData(promptTokens, completionTokens));
    }

    /**
     * 创建错误事件
     */
    public static ExperienceChatEvent error(String message) {
        return new ExperienceChatEvent(EventType.ERROR, new ErrorData(message));
    }

    /**
     * 创建完成事件
     */
    public static ExperienceChatEvent done() {
        return new ExperienceChatEvent(EventType.DONE, null);
    }

    /** 内容数据 */
    public record ContentData(String content) {}

    /** 使用量数据 */
    public record UsageData(int promptTokens, int completionTokens) {}

    /** 错误数据 */
    public record ErrorData(String message) {}
}
