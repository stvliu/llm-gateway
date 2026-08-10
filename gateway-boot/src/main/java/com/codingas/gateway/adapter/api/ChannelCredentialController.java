/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.channelcredential.ChannelCredentialService;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道凭证管理控制器
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/credentials")
@RequiredArgsConstructor
public class ChannelCredentialController {

    private final ChannelCredentialService channelCredentialService;

    /**
     * 获取渠道下的凭证列表
     */
    @GetMapping
    public List<ChannelCredentialResponse> list(@PathVariable Long channelId) {
        return channelCredentialService.listByChannelId(channelId);
    }

    /**
     * 根据 ID 获取凭证详情（含明文，用于页面复制）
     */
    @GetMapping("/{id}")
    public ChannelCredentialDetailResponse get(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        return channelCredentialService.getDetailById(channelId, id);
    }

    /**
     * 创建渠道凭证
     * <p>适配层从路径参数中提取 channelId，补全 DTO 后传给 Service</p>
     */
    @PostMapping
    public ChannelCredentialCreateResponse create(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelCredentialCreateRequest request) {
        // 适配层补全 channelId（不同协议从各自上下文中提取）
        var fullRequest = new ChannelCredentialCreateRequest(
                channelId,
                request.apiKey(),
                request.priority(),
                request.weight(),
                request.description()
        );
        return channelCredentialService.create(fullRequest);
    }

    /**
     * 更新渠道凭证
     * <p>适配层从路径参数中提取 channelId 和 id，补全 DTO 后传给 Service</p>
     */
    @PutMapping("/{id}")
    public ChannelCredentialResponse update(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @Valid @RequestBody ChannelCredentialUpdateRequest request) {
        // 适配层补全 channelId 和 id（不同协议从各自上下文中提取）
        var fullRequest = new ChannelCredentialUpdateRequest(
                channelId,
                id,
                request.priority(),
                request.weight(),
                request.description(),
                request.apiKey()
        );
        return channelCredentialService.update(fullRequest);
    }

    /**
     * 删除渠道凭证
     */
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        channelCredentialService.delete(channelId, id);
    }

    /**
     * 测试 API Key 是否有效
     */
    @PostMapping("/{id}/test")
    public ApiKeyTestResponse testApiKey(
            @PathVariable Long channelId,
            @PathVariable Long id) {
        return channelCredentialService.testApiKey(channelId, id);
    }
}
