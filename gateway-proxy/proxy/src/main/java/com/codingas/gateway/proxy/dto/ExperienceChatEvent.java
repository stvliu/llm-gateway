/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.proxy.dto;

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
