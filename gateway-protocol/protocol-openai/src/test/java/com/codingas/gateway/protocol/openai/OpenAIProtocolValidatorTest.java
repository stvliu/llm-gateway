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
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.validation.ProtocolValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAIProtocolValidator 单元测试：合法/非法请求断言。
 */
class OpenAIProtocolValidatorTest {

    private final OpenAIProtocolValidator validator = new OpenAIProtocolValidator();

    @Test
    void getProtocol_returnsOpenai() {
        assertThat(validator.getProtocol()).isEqualTo("openai");
    }

    @Test
    void validate_validRequest_passes() {
        assertThatCode(() -> validator.validate(validRequest()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_toolRoleWithToolCallId_passes() {
        OpenAIChatRequest req = validRequest();
        req.getMessages().add(OpenAIChatRequest.Message.builder()
                .role("tool")
                .content("{\"city\":\"beijing\"}")
                .toolCallId("call_1")
                .build());
        assertThatCode(() -> validator.validate(req))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_nullModel_throws() {
        OpenAIChatRequest req = validRequest();
        req.setModel(null);
        assertValidationFails(req, "model");
    }

    @Test
    void validate_blankModel_throws() {
        OpenAIChatRequest req = validRequest();
        req.setModel("  ");
        assertValidationFails(req, "model");
    }

    @Test
    void validate_nullMessages_throws() {
        OpenAIChatRequest req = validRequest();
        req.setMessages(null);
        assertValidationFails(req, "messages");
    }

    @Test
    void validate_emptyMessages_throws() {
        OpenAIChatRequest req = validRequest();
        req.setMessages(List.of());
        assertValidationFails(req, "messages");
    }

    @Test
    void validate_invalidRole_throws() {
        OpenAIChatRequest req = validRequest();
        req.getMessages().add(OpenAIChatRequest.Message.builder()
                .role("developer")
                .content("x")
                .build());
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains("role");
                    assertThat(pve.getViolation()).contains("developer");
                });
    }

    @Test
    void validate_nullRole_throws() {
        OpenAIChatRequest req = validRequest();
        req.getMessages().add(OpenAIChatRequest.Message.builder()
                .role(null)
                .content("x")
                .build());
        assertValidationFails(req, "role");
    }

    @Test
    void validate_toolRoleWithoutToolCallId_throws() {
        OpenAIChatRequest req = validRequest();
        req.getMessages().add(OpenAIChatRequest.Message.builder()
                .role("tool")
                .content("{\"ok\":1}")
                .build());
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains("tool_call_id");
                });
    }

    @Test
    void validate_toolRoleBlankToolCallId_throws() {
        OpenAIChatRequest req = validRequest();
        req.getMessages().add(OpenAIChatRequest.Message.builder()
                .role("tool")
                .content("{\"ok\":1}")
                .toolCallId(" ")
                .build());
        assertValidationFails(req, "tool_call_id");
    }

    private OpenAIChatRequest validRequest() {
        // 用可变列表，便于测试追加 tool 消息
        List<OpenAIChatRequest.Message> messages = new ArrayList<>();
        messages.add(OpenAIChatRequest.Message.builder()
                .role("user")
                .content("hi")
                .build());
        return OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(messages)
                .build();
    }

    private void assertValidationFails(OpenAIChatRequest req, String field) {
        assertThatThrownBy(() -> validator.validate(req))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    ProtocolValidationException pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).contains(field);
                });
    }
}
