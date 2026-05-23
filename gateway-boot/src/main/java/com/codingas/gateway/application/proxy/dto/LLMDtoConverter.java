package com.codingas.gateway.application.proxy.dto;

import com.codingas.gateway.domain.proxy.valueobject.LLMRequestVO;
import com.codingas.gateway.domain.proxy.valueobject.LLMResponseVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM DTO 与 Domain 值对象转换工具
 *
 * <p>用于 Application 层 DTO 与 Domain 层值对象之间的转换。</p>
 */
public final class LLMDtoConverter {

    private LLMDtoConverter() {}

    /**
     * 将 Application DTO 转换为 Domain 值对象
     */
    public static LLMRequestVO toVO(LLMRequest dto) {
        if (dto == null) {
            return null;
        }
        return new LLMRequestVO(
                dto.getModel(),
                dto.getProtocol(),
                toMessageVOList(dto.getMessages()),
                dto.getTemperature(),
                dto.getMaxTokens(),
                dto.getStop(),
                dto.getFrequencyPenalty(),
                dto.getPresencePenalty(),
                dto.getResponseFormat(),
                dto.getSeed(),
                toToolDefinitionVOList(dto.getTools()),
                dto.getToolChoice(),
                dto.isStream(),
                dto.getSystemPrompt(),
                dto.getExtraParams(),
                dto.getTimeoutSeconds()
        );
    }

    /**
     * 将 Domain 值对象转换为 Application DTO
     */
    public static LLMResponse toDTO(LLMResponseVO vo) {
        if (vo == null) {
            return null;
        }
        return LLMResponse.builder()
                .provider(vo.provider())
                .model(vo.model())
                .id(vo.id())
                .created(vo.created())
                .content(toContentDTO(vo.content()))
                .usage(toUsageDTO(vo.usage()))
                .finishReason(vo.finishReason())
                .stream(vo.stream())
                .error(toErrorDTO(vo.error()))
                .extraData(vo.extraData())
                .build();
    }

    private static List<LLMRequestVO.MessageVO> toMessageVOList(List<LLMRequest.Message> messages) {
        if (messages == null) {
            return Collections.emptyList();
        }
        return messages.stream()
                .map(m -> new LLMRequestVO.MessageVO(
                        m.getRole(),
                        m.getContent(),
                        toToolCallVOList(m.getToolCalls()),
                        m.getToolCallId(),
                        m.getName()
                ))
                .collect(Collectors.toList());
    }

    private static List<LLMRequestVO.ToolCallVO> toToolCallVOList(List<LLMRequest.ToolCall> toolCalls) {
        if (toolCalls == null) {
            return Collections.emptyList();
        }
        return toolCalls.stream()
                .map(tc -> new LLMRequestVO.ToolCallVO(
                        tc.getId(),
                        tc.getType(),
                        tc.getFunction() != null
                                ? new LLMRequestVO.FunctionCallVO(tc.getFunction().getName(), tc.getFunction().getArguments())
                                : null
                ))
                .collect(Collectors.toList());
    }

    private static List<LLMRequestVO.ToolDefinitionVO> toToolDefinitionVOList(List<LLMRequest.ToolDefinition> tools) {
        if (tools == null) {
            return Collections.emptyList();
        }
        return tools.stream()
                .map(t -> new LLMRequestVO.ToolDefinitionVO(
                        t.getType(),
                        t.getFunction() != null
                                ? new LLMRequestVO.FunctionDefinitionVO(
                                        t.getFunction().getName(),
                                        t.getFunction().getDescription(),
                                        t.getFunction().getParameters())
                                : null
                ))
                .collect(Collectors.toList());
    }

    private static LLMResponse.Content toContentDTO(LLMResponseVO.ContentVO vo) {
        if (vo == null) {
            return null;
        }
        return LLMResponse.Content.builder()
                .text(vo.text())
                .role(vo.role())
                .toolCalls(toToolCallDTOList(vo.toolCalls()))
                .build();
    }

    private static List<LLMResponse.ToolCall> toToolCallDTOList(List<LLMResponseVO.ToolCallVO> toolCalls) {
        if (toolCalls == null) {
            return Collections.emptyList();
        }
        return toolCalls.stream()
                .map(tc -> LLMResponse.ToolCall.builder()
                        .id(tc.id())
                        .type(tc.type())
                        .function(tc.function() != null
                                ? LLMResponse.FunctionCall.builder()
                                        .name(tc.function().name())
                                        .arguments(tc.function().arguments())
                                        .build()
                                : null)
                        .build())
                .collect(Collectors.toList());
    }

    private static LLMResponse.Usage toUsageDTO(LLMResponseVO.UsageVO vo) {
        if (vo == null) {
            return null;
        }
        return LLMResponse.Usage.builder()
                .promptTokens(vo.promptTokens())
                .completionTokens(vo.completionTokens())
                .totalTokens(vo.totalTokens())
                .build();
    }

    private static LLMResponse.Error toErrorDTO(LLMResponseVO.ErrorVO vo) {
        if (vo == null) {
            return null;
        }
        return LLMResponse.Error.builder()
                .type(vo.type())
                .code(vo.code())
                .message(vo.message())
                .param(vo.param())
                .build();
    }
}