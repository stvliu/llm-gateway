package com.codingas.gateway.adapter.api;

import com.codingas.gateway.adapter.api.dto.ChannelHealthCheckRequest;
import com.codingas.gateway.application.channel.ChannelService;
import com.codingas.gateway.application.channel.dto.*;
import com.codingas.gateway.application.supply.ChannelHealthService;
import com.codingas.gateway.application.supply.dto.ChannelHealthResult;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.application.channel.dto.ChannelStateTransitionRequest;
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
    private final ChannelHealthService channelHealthService;

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

    /**
     * 切换渠道状态
     * <p>由后端校验 canTransitionTo() 和前置条件。</p>
     */
    @PutMapping("/{id}/state")
    public void setState(
            @PathVariable Long id,
            @Valid @RequestBody ChannelStateTransitionRequest request) {
        channelService.setState(id, request);
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
        // 适配层补全 channelId（不同协议从各自上下文中提取）
        request.setChannelId(channelId);
        return channelService.addEndpoint(request);
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

    // ===== 健康检查 =====

    /**
     * 触发渠道连通性测试，按聚合规则写入健康状态。
     *
     * <p>仅 CARD / DRAWER 来源持久化健康字段（last-write-wins）；PRECHECK 来源不写库。</p>
     *
     * @param id      渠道 ID
     * @param request 测试参数（含触发来源 source）
     * @return 测试矩阵 + 聚合状态
     */
    @PostMapping("/{id}/health-check")
    public ChannelHealthResult healthCheck(
            @PathVariable Long id,
            @Valid @RequestBody ChannelHealthCheckRequest request) {
        return channelHealthService.check(id, request.source());
    }
}
