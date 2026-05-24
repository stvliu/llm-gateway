package com.codingas.gateway.adapter.api;

import com.codingas.gateway.domain.supply.protocol.OpenAIChatRequest;
import com.codingas.gateway.domain.supply.protocol.OpenAIChatResponse;
import com.codingas.gateway.domain.supply.protocol.ProtocolResponse;
import com.codingas.gateway.application.proxy.ProxyService;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProtocolValidationException;
import com.codingas.gateway.domain.supply.protocol.OpenAIProtocolValidator;
import com.codingas.gateway.domain.iam.valueobject.Identity;
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

    private final ProxyService proxyService;
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
            SseStreamHelper.executeStream(proxyService, request, identity, response);
            return null;
        } else {
            ProtocolResponse protocolResponse = proxyService.proxy(request, identity, RoutingStrategy.WEIGHTED);

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