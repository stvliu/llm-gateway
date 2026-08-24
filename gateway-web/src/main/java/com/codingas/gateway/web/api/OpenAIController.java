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
package com.codingas.gateway.web.api;

import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.proxy.chat.ChatDispatchService;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.protocol.openai.OpenAIProtocolValidator;
import com.codingas.gateway.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * OpenAI 兼容 API 控制器
 *
 * <p>暴露 /v1/chat/completions 端点，兼容 OpenAI API 格式。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAIController {

    private final ChatDispatchService chatDispatchService;
    private final OpenAIProtocolValidator validator;

    /**
     * Chat Completions 端点
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @RequestBody OpenAIChatRequest request,
            @RequestAttribute(value = "identity", required = false) Identity identity,
            HttpServletResponse response) throws IOException {

        log.info("OpenAI chat request: model={}, stream={}", request.getModel(), request.getStream());

        validator.validate(request);

        if (identity == null) {
            throw new IllegalStateException("认证信息缺失，请检查 API Key");
        }

        if (request.isStream()) {
            SseStreamHelper.executeStream(chatDispatchService, request, identity, response);
            return null;
        } else {
            ProtocolResponse protocolResponse = chatDispatchService.dispatch(request, identity, RoutingStrategy.WEIGHTED);

            if (protocolResponse instanceof OpenAIChatResponse openaiResponse) {
                return ResponseEntity.ok(openaiResponse);
            }
            return ResponseEntity.ok(protocolResponse);
        }
    }

    /**
     * 协议校验异常处理
     */
    @ExceptionHandler(ProtocolValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ProtocolValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", Map.of(
                        "message", ex.getMessage(),
                        "type", "invalid_request_error",
                        "code", ex.getField()
                )
        ));
    }
}