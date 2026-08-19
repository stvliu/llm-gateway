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
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

import java.util.List;

/** 规范消息：role + 文本内容 + 工具调用（assistant 侧） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalMessage {

    /** 角色：user / assistant / tool */
    private String role;

    /** 文本内容（Anthropic 多 content block 拼接为 string 的等价表示） */
    private String content;

    /** assistant 消息的工具调用列表（OpenAI tool_calls 等价） */
    private List<CanonicalToolCall> toolCalls;

    /** tool 角色消息关联的工具调用 ID */
    private String toolCallId;

    /** 工具角色名（OpenAI Message.name） */
    private String name;
}
