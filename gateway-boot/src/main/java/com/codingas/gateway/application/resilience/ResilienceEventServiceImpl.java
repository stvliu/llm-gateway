package com.codingas.gateway.application.resilience;

import com.codingas.gateway.application.resilience.dto.FailoverEventResponse;
import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件查询应用服务实现
 *
 * <p>委托 {@link FailoverEventGateway}，将 {@link FailoverEvent} 实体转为
 * {@link FailoverEventResponse} DTO。枚举字段转字符串展示。</p>
 *
 * <p>参照 {@link ResilienceProfileServiceImpl} 的 Service→Gateway 分层范式。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceEventServiceImpl implements ResilienceEventService {

    private final FailoverEventGateway failoverEventGateway;

    @Override
    public List<FailoverEventResponse> findRecent(Instant since, Long applicationId, Long clusterId, int limit) {
        return failoverEventGateway.findRecent(since, applicationId, clusterId, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<FailoverEventResponse> findExhausted(Instant since, int limit) {
        return failoverEventGateway.findExhausted(since, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 实体转响应 DTO（枚举转字符串展示）
     */
    private FailoverEventResponse toResponse(FailoverEvent event) {
        FailoverEventResponse response = new FailoverEventResponse();
        response.setId(event.getId());
        response.setTraceId(event.getTraceId());
        response.setApplicationId(event.getApplicationId());
        response.setFromChannelId(event.getFromChannelId());
        response.setFromEndpointId(event.getFromEndpointId());
        response.setToChannelId(event.getToChannelId());
        response.setToEndpointId(event.getToEndpointId());
        response.setFromClusterId(event.getFromClusterId());
        response.setToClusterId(event.getToClusterId());
        response.setErrorType(event.getErrorType() != null ? event.getErrorType().name() : null);
        response.setDecision(event.getDecision() != null ? event.getDecision().name() : null);
        response.setExhausted(event.isExhausted());
        response.setOccurredAt(event.getOccurredAt());
        return response;
    }
}
