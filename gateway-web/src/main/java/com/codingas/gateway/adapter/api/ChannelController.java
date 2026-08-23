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

import com.codingas.gateway.adapter.api.dto.ChannelHealthCheckRequest;
import com.codingas.gateway.provider.service.ChannelEmergencyService;
import com.codingas.gateway.provider.service.ChannelService;
import com.codingas.gateway.provider.channel.*;
import com.codingas.gateway.provider.service.ChannelHealthService;
import com.codingas.gateway.provider.dto.ChannelHealthResult;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.channel.ChannelStateTransitionRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
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
    private final ChannelEmergencyService channelEmergencyService;

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

    // ===== 应急操作 =====

    /**
     * 一键熔断端点
     *
     * <p>运维应急：强制端点熔断器进入 OPEN，立即切断该端点流量。
     * 用于故障应急时快速隔离问题端点。</p>
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 熔断后的状态
     */
    @PostMapping("/{channelId}/endpoints/{endpointId}/circuit-breaker/force-open")
    public CircuitBreakerStateResponse forceOpen(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        return new CircuitBreakerStateResponse(channelEmergencyService.forceOpen(channelId, endpointId));
    }

    /**
     * 一键恢复端点
     *
     * <p>运维应急：强制端点熔断器回到 CLOSED 并重置窗口，立即恢复流量。
     * 用于故障修复后快速恢复端点。</p>
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 恢复后的状态
     */
    @PostMapping("/{channelId}/endpoints/{endpointId}/circuit-breaker/force-close")
    public CircuitBreakerStateResponse forceClose(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        return new CircuitBreakerStateResponse(channelEmergencyService.forceClose(channelId, endpointId));
    }

    /**
     * 查询端点熔断器状态
     *
     * @param channelId   渠道 ID
     * @param endpointId  端点 ID（须属于该渠道）
     * @return 当前熔断器状态
     */
    @GetMapping("/{channelId}/endpoints/{endpointId}/circuit-breaker/state")
    public CircuitBreakerStateResponse getCircuitBreakerState(
            @PathVariable Long channelId,
            @PathVariable Long endpointId) {
        return new CircuitBreakerStateResponse(channelEmergencyService.getState(channelId, endpointId));
    }

    /**
     * 熔断器状态响应 DTO
     */
    @Data
    @AllArgsConstructor
    public static class CircuitBreakerStateResponse {
        /** 熔断器状态名（CLOSED/OPEN/HALF_OPEN） */
        private String state;
    }
}
