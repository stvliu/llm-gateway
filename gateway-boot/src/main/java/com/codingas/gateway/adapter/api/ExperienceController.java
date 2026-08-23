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
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.proxy.experience.ModelExperienceService;
import com.codingas.gateway.proxy.dto.ExperienceChatRequest;
import com.codingas.gateway.proxy.dto.ExperienceModelResponse;
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
