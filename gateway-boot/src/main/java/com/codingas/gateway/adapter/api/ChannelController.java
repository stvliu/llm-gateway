package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.channel.dto.*;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request) {
        ChannelResponse response = channelService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelRequest request) {
        ChannelResponse response = channelService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponse> getById(@PathVariable Long id) {
        ChannelResponse response = channelService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有渠道列表（用于渠道管理页面）
     * 不带 providerId 参数时返回所有渠道
     */
    @GetMapping
    public ResponseEntity<List<ChannelResponse>> list(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String billingMode) {
        List<ChannelResponse> responses;

        if (providerId == null) {
            // 获取所有渠道
            responses = channelService.getAll();
        } else if (billingMode != null) {
            // 按 providerId 和 billingMode 筛选
            responses = channelService.getByProviderIdAndBillingMode(
                providerId, BillingMode.fromCode(billingMode));
        } else {
            // 按 providerId 筛选
            responses = channelService.getByProviderId(providerId);
        }

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== 端点管理 =====

    @PostMapping("/{channelId}/endpoints")
    public ResponseEntity<ChannelEndpointResponse> addEndpoint(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelEndpointRequest request) {
        ChannelEndpointResponse response = channelService.addEndpoint(channelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{channelId}/endpoints/{endpointId}")
    public ResponseEntity<Void> removeEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        channelService.removeEndpoint(channelId, endpointId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{channelId}/endpoints/{endpointId}/enable")
    public ResponseEntity<ChannelEndpointResponse> enableEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        ChannelEndpointResponse response = channelService.enableEndpoint(channelId, endpointId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{channelId}/endpoints/{endpointId}/disable")
    public ResponseEntity<ChannelEndpointResponse> disableEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        ChannelEndpointResponse response = channelService.disableEndpoint(channelId, endpointId);
        return ResponseEntity.ok(response);
    }
}
