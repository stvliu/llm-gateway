package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIProtocolValidatorTest {

    private final OpenAIProtocolValidator validator = new OpenAIProtocolValidator();

    @Test
    void shouldPassValidRequest() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("user").content("hello").build()))
                .build();
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNullModel() {
        var request = OpenAIChatRequest.builder()
                .model(null)
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("user").content("hello").build()))
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("model");
                });
    }

    @Test
    void shouldRejectEmptyMessages() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(null)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("messages");
                });
    }

    @Test
    void shouldRejectInvalidRole() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("invalid_role").content("hello").build()))
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("messages[0].role");
                });
    }
}