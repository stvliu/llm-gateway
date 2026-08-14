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
package com.codingas.gateway.adapter.protocol.anthropic;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnthropicTuner 出站调谐器单元测试
 *
 * <p>该调谐器承担 max_tokens 缺省补 1024 与 system 角色消息提取职责（自旧
 * {@code ProtocolConverter} 下沉而来）。在转换层不再补 1024 后，这里是该行为的唯一
 * 自动化回归守卫。</p>
 */
class AnthropicTunerTest {

    private final AnthropicTuner tuner = new AnthropicTuner();

    @Test
    @DisplayName("max_tokens 缺省补 1024")
    void maxTokensNull_defaultsTo1024() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest result = tuner.tune(request);

        assertThat(result.getMaxTokens()).isEqualTo(1024);
    }

    @Test
    @DisplayName("max_tokens 已有值保持不变")
    void maxTokensPresent_unchanged() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .maxTokens(512)
                .build();

        AnthropicMessagesRequest result = tuner.tune(request);

        assertThat(result.getMaxTokens()).isEqualTo(512);
    }

    @Test
    @DisplayName("system 角色消息提取到顶层 system 字段并从 messages 移除")
    void systemMessage_extractedToTopLevel() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder()
                                .role("system").content("你是助手").build(),
                        AnthropicMessagesRequest.Message.builder()
                                .role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest result = tuner.tune(request);

        assertThat(result.getSystem()).isEqualTo("你是助手");
        assertThat(result.getMessages()).hasSize(1);
        assertThat(result.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    @DisplayName("顶层 system 已存在时不重复提取")
    void existingTopLevelSystem_kept() {
        AnthropicMessagesRequest request = AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4")
                .system("顶层系统提示")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder()
                                .role("system").content("消息内系统提示").build(),
                        AnthropicMessagesRequest.Message.builder()
                                .role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest result = tuner.tune(request);

        // 顶层 system 已有值，保持不动；消息列表原样保留
        assertThat(result.getSystem()).isEqualTo("顶层系统提示");
        assertThat(result.getMessages()).hasSize(2);
    }
}
