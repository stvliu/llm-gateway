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

/** 规范内容块：type=text 或 type=toolUse */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalContentBlock {

    /** "text" | "toolUse" */
    private String type;

    /** type=text 时的文本 */
    private String text;

    /** type=toolUse 时的工具调用 */
    private CanonicalToolCall toolUse;
}
