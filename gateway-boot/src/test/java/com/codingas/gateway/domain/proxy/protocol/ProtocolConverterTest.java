package com.codingas.gateway.domain.proxy.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConverterTest {

    private final ProtocolConverter converter = new ProtocolConverter(new ObjectMapper());

    @Nested
    class RequestConversion {

        @Test
        void shouldConvertOpenAIToAnthropic_basicRequest() {
            var openai = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(
                            OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(2048)
                    .temperature(0.7)
                    .stream(true)
                    .build();

            var anthropic = converter.toAnthropic(openai);

            assertThat(anthropic.getModel()).isEqualTo("gpt-4o");
            assertThat(anthropic.getMaxTokens()).isEqualTo(2048);
            assertThat(anthropic.getTemperature()).isEqualTo(0.7);
            assertThat(anthropic.isStream()).isTrue();
            assertThat(anthropic.getMessages()).hasSize(1);
            assertThat(anthropic.getMessages().get(0).getRole()).isEqualTo("user");
        }

        @Test
        void shouldConvertOpenAIToAnthropic_systemMessageExtracted() {
            var openai = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(
                            OpenAIChatRequest.Message.builder().role("system").content("You are helpful").build(),
                            OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(2048)
                    .build();

            var anthropic = converter.toAnthropic(openai);

            assertThat(anthropic.getSystem()).isEqualTo("You are helpful");
            assertThat(anthropic.getMessages()).hasSize(1);
            assertThat(anthropic.getMessages().get(0).getRole()).isEqualTo("user");
        }

        @Test
        void shouldConvertOpenAIToAnthropic_maxTokensDefaultWhenNull() {
            var openai = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(
                            OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(null)
                    .build();

            var anthropic = converter.toAnthropic(openai);

            assertThat(anthropic.getMaxTokens()).isEqualTo(1024);
        }

        @Test
        void shouldConvertOpenAIToAnthropic_toolsPassedThrough() {
            var tools = List.of(Map.<String, Object>of("type", "function", "name", "get_weather"));
            var openai = OpenAIChatRequest.builder()
                    .model("gpt-4o")
                    .messages(List.of(OpenAIChatRequest.Message.builder().role("user").content("weather").build()))
                    .tools(tools)
                    .toolChoice("auto")
                    .build();

            var anthropic = converter.toAnthropic(openai);

            assertThat(anthropic.getTools()).isEqualTo(tools);
            assertThat(anthropic.getToolChoice()).isNotNull();
            assertThat(anthropic.getToolChoice().get("type")).isEqualTo("auto");
        }

        @Test
        void shouldConvertAnthropicToOpenAI_basicRequest() {
            var anthropic = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(
                            AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(1024)
                    .temperature(0.5)
                    .stream(true)
                    .build();

            var openai = converter.toOpenAI(anthropic);

            assertThat(openai.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(openai.getMaxTokens()).isEqualTo(1024);
            assertThat(openai.getTemperature()).isEqualTo(0.5);
            assertThat(openai.isStream()).isTrue();
            assertThat(openai.getMessages()).hasSize(1);
            assertThat(openai.getMessages().get(0).getRole()).isEqualTo("user");
        }

        @Test
        void shouldConvertAnthropicToOpenAI_systemMergedToMessages() {
            var anthropic = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .system("You are helpful")
                    .messages(List.of(
                            AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                    .maxTokens(1024)
                    .build();

            var openai = converter.toOpenAI(anthropic);

            assertThat(openai.getMessages()).hasSize(2);
            assertThat(openai.getMessages().get(0).getRole()).isEqualTo("system");
            assertThat(openai.getMessages().get(0).getContent()).isEqualTo("You are helpful");
            assertThat(openai.getMessages().get(1).getRole()).isEqualTo("user");
        }

        @Test
        void shouldConvertAnthropicToOpenAI_toolsAndToolChoicePassedThrough() {
            var tools = List.of(Map.<String, Object>of("type", "function", "name", "get_weather"));
            var anthropic = AnthropicMessagesRequest.builder()
                    .model("claude-3-5-sonnet-20241022")
                    .messages(List.of(AnthropicMessagesRequest.Message.builder().role("user").content("weather").build()))
                    .maxTokens(1024)
                    .tools(tools)
                    .toolChoice(Map.of("type", "auto"))
                    .build();

            var openai = converter.toOpenAI(anthropic);

            assertThat(openai.getTools()).isEqualTo(tools);
            assertThat(openai.getToolChoice()).isEqualTo("auto");
        }
    }

    @Nested
    class ResponseConversion {

        @Test
        void shouldConvertOpenAIToAnthropic_response() {
            var openai = OpenAIChatResponse.builder()
                    .id("chatcmpl-123")
                    .model("gpt-4o")
                    .choices(List.of(OpenAIChatResponse.Choice.builder()
                            .index(0)
                            .message(OpenAIChatResponse.Message.builder()
                                    .role("assistant").content("Hello!").build())
                            .finishReason("stop")
                            .build()))
                    .usage(OpenAIChatResponse.Usage.builder()
                            .promptTokens(10).completionTokens(5).totalTokens(15).build())
                    .build();

            var anthropic = converter.toAnthropic(openai);

            assertThat(anthropic.getModel()).isEqualTo("gpt-4o");
            assertThat(anthropic.getStopReason()).isEqualTo("end_turn");
            assertThat(anthropic.getContent()).hasSize(1);
            assertThat(anthropic.getContent().get(0).getText()).isEqualTo("Hello!");
            assertThat(anthropic.getUsage().getInputTokens()).isEqualTo(10);
            assertThat(anthropic.getUsage().getOutputTokens()).isEqualTo(5);
        }

        @Test
        void shouldConvertAnthropicToOpenAI_response() {
            var anthropic = AnthropicMessagesResponse.builder()
                    .id("msg-123")
                    .model("claude-3-5-sonnet-20241022")
                    .stopReason("end_turn")
                    .content(List.of(AnthropicMessagesResponse.ContentBlock.builder()
                            .type("text").text("Hello!").build()))
                    .usage(AnthropicMessagesResponse.Usage.builder()
                            .inputTokens(10).outputTokens(5).build())
                    .build();

            var openai = converter.toOpenAI(anthropic);

            assertThat(openai.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
            assertThat(openai.getChoices().get(0).getFinishReason()).isEqualTo("stop");
            assertThat(openai.getChoices().get(0).getMessage().getContent()).isEqualTo("Hello!");
            assertThat(openai.getUsage().getTotalTokens()).isEqualTo(15);
        }

        @Test
        void shouldMapFinishReasonsCorrectly() {
            assertThat(converter.toAnthropic(OpenAIChatResponse.builder()
                    .id("1").model("m")
                    .choices(List.of(OpenAIChatResponse.Choice.builder()
                            .index(0).finishReason("stop").build()))
                    .build()).getStopReason()).isEqualTo("end_turn");

            assertThat(converter.toAnthropic(OpenAIChatResponse.builder()
                    .id("1").model("m")
                    .choices(List.of(OpenAIChatResponse.Choice.builder()
                            .index(0).finishReason("length").build()))
                    .build()).getStopReason()).isEqualTo("max_tokens");

            assertThat(converter.toAnthropic(OpenAIChatResponse.builder()
                    .id("1").model("m")
                    .choices(List.of(OpenAIChatResponse.Choice.builder()
                            .index(0).finishReason("tool_calls").build()))
                    .build()).getStopReason()).isEqualTo("tool_use");
        }
    }

    @Nested
    class StreamConversion {

        @Test
        void shouldConvertOpenAIChunkToAnthropic_withEventType() {
            String openaiChunk = "{\"id\":\"chatcmpl-1\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hi\"},\"finish_reason\":null}]}";

            StreamChunkResult result = converter.convertStreamChunk(openaiChunk, "openai", "anthropic");

            assertThat(result).isNotNull();
            assertThat(result.eventType()).isEqualTo("content_block_delta");
            assertThat(result.data()).contains("content_block_delta");
            assertThat(result.data()).contains("Hi");
        }

        @Test
        void shouldConvertAnthropicChunkToOpenAI_noEventType() {
            String anthropicChunk = "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hi\"}}";

            StreamChunkResult result = converter.convertStreamChunk(anthropicChunk, "anthropic", "openai");

            assertThat(result).isNotNull();
            assertThat(result.eventType()).isNull();
            assertThat(result.data()).contains("choices");
            assertThat(result.data()).contains("Hi");
        }

        @Test
        void shouldReturnNullForUnconvertibleChunk() {
            assertThat(converter.convertStreamChunk("", "openai", "anthropic")).isNull();
            assertThat(converter.convertStreamChunk(null, "openai", "anthropic")).isNull();
        }

        @Test
        void shouldConvertStreamDone() {
            StreamChunkResult result = converter.convertStreamDone("openai", "anthropic");
            assertThat(result.eventType()).isEqualTo("message_delta");
            assertThat(result.data()).contains("message_delta");

            StreamChunkResult result2 = converter.convertStreamDone("anthropic", "openai");
            assertThat(result2.eventType()).isNull();
            assertThat(result2.data()).isEqualTo("[DONE]");
        }

        @Test
        void shouldPassThroughSameProtocolChunk() {
            String chunk = "{\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}";
            StreamChunkResult result = converter.convertStreamChunk(chunk, "openai", "openai");
            assertThat(result.eventType()).isNull();
            assertThat(result.data()).isEqualTo(chunk);
        }

        @Test
        void shouldConvertAnthropicMessageDeltaToOpenAI() {
            String anthropicChunk = "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}}";

            StreamChunkResult result = converter.convertStreamChunk(anthropicChunk, "anthropic", "openai");

            assertThat(result).isNotNull();
            assertThat(result.data()).contains("finish_reason");
            assertThat(result.data()).contains("stop");
        }
    }
}
