package com.codingas.gateway.domain.proxy.valueobject;

import java.util.List;
import java.util.Map;

/**
 * LLM 请求值对象（Domain 层）
 *
 * <p>统一的 LLM 请求格式，用于协议网关接口。</p>
 * <p>Application 层负责将 Application DTO 转换为此值对象。</p>
 */
public record LLMRequestVO(
        String model,
        String protocol,
        List<MessageVO> messages,
        Double temperature,
        Integer maxTokens,
        List<String> stop,
        Double frequencyPenalty,
        Double presencePenalty,
        String responseFormat,
        Integer seed,
        List<ToolDefinitionVO> tools,
        String toolChoice,
        boolean stream,
        String systemPrompt,
        Map<String, Object> extraParams,
        Integer timeoutSeconds
) {

    /**
     * 消息值对象
     */
    public record MessageVO(
            String role,
            String content,
            List<ToolCallVO> toolCalls,
            String toolCallId,
            String name
    ) {}

    /**
     * 工具调用值对象
     */
    public record ToolCallVO(
            String id,
            String type,
            FunctionCallVO function
    ) {}

    /**
     * 函数调用值对象
     */
    public record FunctionCallVO(
            String name,
            String arguments
    ) {}

    /**
     * 工具定义值对象
     */
    public record ToolDefinitionVO(
            String type,
            FunctionDefinitionVO function
    ) {}

    /**
     * 函数定义值对象
     */
    public record FunctionDefinitionVO(
            String name,
            String description,
            String parameters
    ) {}
}