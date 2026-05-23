package com.codingas.gateway.domain.proxy.valueobject;

import java.util.List;
import java.util.Map;

/**
 * LLM 响应值对象（Domain 层）
 *
 * <p>统一的 LLM 响应格式，用于协议网关接口。</p>
 * <p>Application 层负责将此值对象转换为 Application DTO。</p>
 */
public record LLMResponseVO(
        String provider,
        String model,
        String id,
        Long created,
        ContentVO content,
        UsageVO usage,
        String finishReason,
        boolean stream,
        ErrorVO error,
        Map<String, Object> extraData
) {

    /**
     * 消息内容值对象
     */
    public record ContentVO(
            String text,
            List<ToolCallVO> toolCalls,
            String role
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
     * Token 使用量值对象
     */
    public record UsageVO(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {}

    /**
     * 错误值对象
     */
    public record ErrorVO(
            String type,
            String code,
            String message,
            String param
    ) {}

    /**
     * 创建错误响应
     */
    public static LLMResponseVO error(String provider, String message) {
        return new LLMResponseVO(
                provider, null, null, null, null, null, null, false,
                new ErrorVO(null, null, message, null), null
        );
    }
}