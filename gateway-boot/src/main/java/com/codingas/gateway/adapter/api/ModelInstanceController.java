/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ModelInstanceService;
import com.codingas.gateway.application.channel.dto.ModelInstanceCreateRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceUpdateRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceStateTransitionRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型实例 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/models")
@RequiredArgsConstructor
public class ModelInstanceController {

    private final ModelInstanceService modelInstanceService;

    @GetMapping
    public List<ModelInstanceResponse> list(@PathVariable Long channelId) {
        return modelInstanceService.getInstancesByChannelId(channelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModelInstanceResponse create(
            @PathVariable Long channelId,
            @Valid @RequestBody ModelInstanceCreateRequest request) {
        // 适配层补全 channelId（不同协议从各自上下文中提取）
        request.setChannelId(channelId);
        return modelInstanceService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long channelId, @PathVariable Long id) {
        modelInstanceService.delete(channelId, id);
    }

    @PutMapping("/{id}/state")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ModelInstanceStateTransitionRequest request) {
        modelInstanceService.setEnabled(channelId, id, request);
    }

    @PatchMapping("/{id}/upstream-model-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUpstreamModelName(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String upstreamModelName = body.get("upstreamModelName");
        modelInstanceService.updateUpstreamModelName(channelId, id, upstreamModelName);
    }

    /**
     * 更新模型实例（支持修改 modelId 和 upstreamModelName）
     *
     * <p>字段为 null 表示不更新该字段。</p>
     */
    @PutMapping("/{id}")
    public ModelInstanceResponse update(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ModelInstanceUpdateRequest request) {
        request.setChannelId(channelId);
        return modelInstanceService.update(channelId, id, request);
    }
}