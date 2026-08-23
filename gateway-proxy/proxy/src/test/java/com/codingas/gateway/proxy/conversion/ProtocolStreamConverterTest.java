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
package com.codingas.gateway.proxy.conversion;

import com.codingas.gateway.protocol.StreamChunkResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProtocolStreamConverter 单元测试（自旧转换逻辑的流式部分平移）
 *
 * <p>覆盖 OpenAI ↔ Anthropic 的 SSE chunk 转换与结束标记转换，行为保持原样（纯平移，不改语义）。</p>
 */
class ProtocolStreamConverterTest {

    private ProtocolStreamConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ProtocolStreamConverter(new ObjectMapper());
    }

    @Nested
    @DisplayName("chunk 转换")
    class ChunkConversion {

        @Test
        @DisplayName("null 或空白 chunk 返回 null")
        void nullOrBlankChunkReturnsNull() {
            assertThat(converter.convertStreamChunk(null, "openai", "anthropic")).isNull();
            assertThat(converter.convertStreamChunk("  ", "openai", "anthropic")).isNull();
        }

        @Test
        @DisplayName("同协议 chunk 原样透传（dataOnly）")
        void sameProtocolPassthrough() {
            String chunk = "{\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}";
            StreamChunkResult result = converter.convertStreamChunk(chunk, "openai", "openai");
            assertThat(result).isNotNull();
            assertThat(result.eventType()).isNull();
            assertThat(result.data()).isEqualTo(chunk);
        }

        @Test
        @DisplayName("非法 JSON chunk 返回 null")
        void invalidJsonReturnsNull() {
            assertThat(converter.convertStreamChunk("not-json", "openai", "anthropic")).isNull();
        }

        @Test
        @DisplayName("OpenAI chunk → Anthropic：content_block_delta 事件")
        void openaiChunkToAnthropic() {
            String chunk = "{\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}";
            StreamChunkResult result = converter.convertStreamChunk(chunk, "openai", "anthropic");
            assertThat(result).isNotNull();
            assertThat(result.eventType()).isEqualTo("content_block_delta");
            assertThat(result.data()).contains("\"type\":\"content_block_delta\"")
                    .contains("\"text\":\"hi\"");
        }

        @Test
        @DisplayName("OpenAI chunk 无 content 时返回 null")
        void openaiChunkWithoutContentReturnsNull() {
            String chunk = "{\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}";
            assertThat(converter.convertStreamChunk(chunk, "openai", "anthropic")).isNull();
        }

        @Test
        @DisplayName("Anthropic content_block_delta → OpenAI：chat.completion.chunk")
        void anthropicContentDeltaToOpenAI() {
            String chunk = "{\"type\":\"content_block_delta\",\"delta\":{\"text\":\"hi\"}}";
            StreamChunkResult result = converter.convertStreamChunk(chunk, "anthropic", "openai");
            assertThat(result).isNotNull();
            assertThat(result.eventType()).isNull();
            assertThat(result.data()).contains("chat.completion.chunk")
                    .contains("\"content\":\"hi\"");
        }

        @Test
        @DisplayName("Anthropic message_delta → OpenAI：stop_reason 映射为 finish_reason")
        void anthropicMessageDeltaToOpenAI() {
            String chunk = "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}}";
            StreamChunkResult result = converter.convertStreamChunk(chunk, "anthropic", "openai");
            assertThat(result).isNotNull();
            assertThat(result.data()).contains("chat.completion.chunk")
                    .contains("\"finish_reason\":\"stop\"");
        }

        @Test
        @DisplayName("Anthropic 未知类型 chunk 返回 null")
        void anthropicUnknownTypeReturnsNull() {
            String chunk = "{\"type\":\"message_start\"}";
            assertThat(converter.convertStreamChunk(chunk, "anthropic", "openai")).isNull();
        }
    }

    @Nested
    @DisplayName("结束标记转换")
    class DoneConversion {

        @Test
        @DisplayName("OpenAI 结束 → Anthropic：message_delta 事件（end_turn）")
        void openaiDoneToAnthropic() {
            StreamChunkResult result = converter.convertStreamDone("openai", "anthropic");
            assertThat(result).isNotNull();
            assertThat(result.eventType()).isEqualTo("message_delta");
            assertThat(result.data()).contains("\"stop_reason\":\"end_turn\"");
        }

        @Test
        @DisplayName("Anthropic 结束 → OpenAI：[DONE]")
        void anthropicDoneToOpenAI() {
            StreamChunkResult result = converter.convertStreamDone("anthropic", "openai");
            assertThat(result).isNotNull();
            assertThat(result.eventType()).isNull();
            assertThat(result.data()).isEqualTo("[DONE]");
        }

        @Test
        @DisplayName("同协议/未知方向结束标记返回 null")
        void unknownDirectionDoneReturnsNull() {
            assertThat(converter.convertStreamDone("openai", "openai")).isNull();
            assertThat(converter.convertStreamDone("anthropic", "anthropic")).isNull();
        }
    }
}
