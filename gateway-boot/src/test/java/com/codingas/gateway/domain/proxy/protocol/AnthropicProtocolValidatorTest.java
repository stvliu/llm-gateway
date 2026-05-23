package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicProtocolValidatorTest {

    private final AnthropicProtocolValidator validator = new AnthropicProtocolValidator();

    @Test
    void shouldPassValidRequest() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNullModel() {
        var request = AnthropicMessagesRequest.builder()
                .model(null)
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("model"));
    }

    @Test
    void shouldRejectNullMaxTokens() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(null)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("max_tokens"));
    }

    @Test
    void shouldRejectZeroMaxTokens() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(0)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("max_tokens"));
    }

    @Test
    void shouldRejectEmptyMessages() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(null)
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("messages"));
    }

    @Test
    void shouldRejectSystemRoleInMessages() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder().role("system").content("system prompt").build(),
                        AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("messages[0].role"));
    }

    @Test
    void shouldRejectFirstMessageNotUser() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(
                        AnthropicMessagesRequest.Message.builder().role("assistant").content("prev").build(),
                        AnthropicMessagesRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("messages[0].role"));
    }

    @Test
    void shouldPassWithTopLevelSystemField() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .system("You are helpful")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }
}