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
package com.codingas.gateway.protocol.canonical;

import lombok.*;

import java.util.List;

/**
 * 规范聊天请求（Canonical IR）——与厂商无关的中立表示。
 *
 * <p>用于协议适配层：入站原生请求 normalize 为规范模型，出站由规范模型
 * denormalize 为上游原生请求。任意两协议互转 = normalize + denormalize 两跳。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalChatRequest {

    /** 模型名（用户面，路由后可能被上游模型名覆盖） */
    private String model;

    /** system 顶层指令（Anthropic 风格；OpenAI 由 system 角色消息提取） */
    private String system;

    /** 消息列表（不含 system 角色） */
    private List<CanonicalMessage> messages;

    /** 最大输出 token，null 表示未指定 */
    private Integer maxTokens;

    /** 采样温度，null 表示未指定 */
    private Double temperature;

    /** 停止序列 */
    private List<String> stop;

    /** 工具列表（function calling） */
    private List<CanonicalTool> tools;

    /** tool_choice 类型（"auto"/"required"/"none"/指定工具名） */
    private String toolChoice;

    /** 是否流式 */
    private boolean stream;
}
