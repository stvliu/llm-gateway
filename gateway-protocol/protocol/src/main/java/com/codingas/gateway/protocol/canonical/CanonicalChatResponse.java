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

/** 规范聊天响应——中立表示，覆盖 text 与 tool_use 两种 content block */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalChatResponse {

    private String id;
    private String model;

    /** 内容块列表（text / toolUse） */
    private List<CanonicalContentBlock> content;

    /** 停止原因（end_turn / max_tokens / tool_use 等规范值） */
    private String stopReason;

    /** token 用量 */
    private CanonicalUsage usage;
}
