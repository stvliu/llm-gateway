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
package com.codingas.gateway.protocol.anthropic;

import com.codingas.gateway.protocol.raw.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AnthropicProtocolValidator 单元测试：合法/非法请求断言。
 */
class AnthropicProtocolValidatorTest {

    private final AnthropicProtocolValidator validator = new AnthropicProtocolValidator();

    @Test
    void getProtocol_returnsAnthropic() {
        assertThat(validator.getProtocol()).isEqualTo("anthropic");
    }

    @Test
    void validate_validRequest_passes() {
        assertThatCode(() -> validator.validate(validRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_nullModel_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setModel(null);
        assertValidationFails(req, "model");
    }

    @Test
    void validate_blankModel_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setModel(" ");
        assertValidationFails(req, "model");
    }

    @Test
    void validate_nullMaxTokens_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMaxTokens(null);
        assertValidationFails(req, "max_tokens");
    }

    @Test
    void validate_zeroMaxTokens_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMaxTokens(0);
        assertValidationFails(req, "max_tokens");
    }

    @Test
    void validate_negativeMaxTokens_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMaxTokens(-1);
        assertValidationFails(req, "max_tokens");
    }

    @Test
    void validate_nullMessages_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMessages(null);
        assertValidationFails(req, "messages");
    }

    @Test
    void validate_emptyMessages_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMessages(List.of());
        assertValidationFails(req, "messages");
    }

    @Test
    void validate_systemRoleInMessages_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMessages(List.of(
                AnthropicMessagesRequest.Message.builder().role("user").content("hi").build(),
                AnthropicMessagesRequest.Message.builder().role("system").content("sys").build()));
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains("role");
                    assertThat(pve.getViolation()).contains("system 角色应使用顶层 system 字段");
                });
    }

    @Test
    void validate_firstMessageNotUser_throws() {
        AnthropicMessagesRequest req = validRequest();
        req.setMessages(List.of(
                AnthropicMessagesRequest.Message.builder().role("assistant").content("hi").build()));
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains("messages[0].role");
                });
    }

    private AnthropicMessagesRequest validRequest() {
        return AnthropicMessagesRequest.builder()
                .model("claude-sonnet-4-20250514")
                .maxTokens(100)
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user")
                        .content("hi")
                        .build()))
                .build();
    }

    private void assertValidationFails(AnthropicMessagesRequest req, String field) {
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains(field);
                });
    }
}
