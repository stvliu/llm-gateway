/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.application.proxy.ChatDispatchService;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.adapter.protocol.anthropic.AnthropicProtocolValidator;
import com.codingas.gateway.domain.protocol.validation.ProtocolValidationException;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Anthropic 兼容 API 控制器
 *
 * <p>暴露 /anthropic/v1/messages 端点，兼容 Anthropic API 格式。</p>
 */
@Slf4j
@RestController
@RequestMapping("/anthropic/v1")
@RequiredArgsConstructor
public class AnthropicController {

    private final ChatDispatchService chatDispatchService;
    private final AnthropicProtocolValidator validator;

    /**
     * Messages 端点
     */
    @PostMapping("/messages")
    public ResponseEntity<?> messages(
            @RequestBody AnthropicMessagesRequest request,
            @RequestAttribute(value = "identity", required = false) Identity identity,
            HttpServletResponse response) throws IOException {

        log.info("Anthropic messages request: model={}, stream={}", request.getModel(), request.getStream());

        validator.validate(request);

        if (identity == null) {
            throw new IllegalStateException("认证信息缺失，请检查 API Key");
        }

        if (request.isStream()) {
            SseStreamHelper.executeStream(chatDispatchService, request, identity, response);
            return null;
        } else {
            ProtocolResponse protocolResponse = chatDispatchService.dispatch(request, identity, RoutingStrategy.WEIGHTED);

            if (protocolResponse instanceof AnthropicMessagesResponse anthropicResponse) {
                return ResponseEntity.ok(anthropicResponse);
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
                "type", "error",
                "error", Map.of(
                        "type", "invalid_request_error",
                        "message", ex.getMessage()
                )
        ));
    }
}