package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.experience.ModelExperienceService;
import com.codingas.gateway.application.experience.dto.ExperienceChatRequest;
import com.codingas.gateway.application.experience.dto.ExperienceModelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 模型体验 Controller
 *
 * <p>提供模型体验功能，支持流式聊天。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/experience")
@RequiredArgsConstructor
public class ExperienceController {

    private final ModelExperienceService modelExperienceService;

    /**
     * 流式聊天体验
     *
     * <p>通过 SSE 返回流式响应。</p>
     *
     * @param request 聊天请求
     * @return SSE Emitter
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ExperienceChatRequest request) {
        log.info("Experience chat request: channelId={}, model={}",
            request.getChannelId(), request.getModel());
        return modelExperienceService.chatStream(request);
    }

    /**
     * 获取供应商的模型列表
     *
     * @param providerId 供应商 ID
     * @return 模型列表
     */
    @GetMapping("/providers/{providerId}/models")
    public List<ExperienceModelResponse> getProviderModels(@PathVariable Long providerId) {
        return modelExperienceService.getModelsByProviderId(providerId);
    }
}
