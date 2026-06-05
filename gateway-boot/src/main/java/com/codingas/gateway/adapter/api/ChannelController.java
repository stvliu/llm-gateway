package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.channel.dto.*;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse create(@Valid @RequestBody ChannelRequest request) {
        return channelService.create(request);
    }

    @PutMapping("/{id}")
    public ChannelResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelRequest request) {
        return channelService.update(id, request);
    }

    @GetMapping("/{id}")
    public ChannelResponse getById(@PathVariable Long id) {
        return channelService.getById(id);
    }

    /**
     * 获取所有渠道列表（用于渠道管理页面）
     * 不带 providerId 参数时返回所有渠道
     */
    @GetMapping
    public List<ChannelResponse> list(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String billingMode) {
        if (providerId == null) {
            return channelService.getAll();
        } else if (billingMode != null) {
            return channelService.getByProviderIdAndBillingMode(
                providerId, BillingMode.fromCode(billingMode));
        } else {
            return channelService.getByProviderId(providerId);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        channelService.delete(id);
    }

    // ===== 端点管理 =====

    @PostMapping("/{channelId}/endpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelEndpointResponse addEndpoint(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelEndpointRequest request) {
        return channelService.addEndpoint(channelId, request);
    }

    @DeleteMapping("/{channelId}/endpoints/{endpointId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        channelService.removeEndpoint(channelId, endpointId);
    }

    @PutMapping("/{channelId}/endpoints/{endpointId}")
    public ChannelEndpointResponse updateEndpoint(
            @PathVariable Long channelId,
            @PathVariable Long endpointId,
            @Valid @RequestBody ChannelEndpointRequest request) {
        return channelService.updateEndpoint(channelId, endpointId, request);
    }
}
