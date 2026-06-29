package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.FailoverEventDo;
import com.codingas.gateway.infrastructure.resilience.gateway.database.repository.FailoverEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件领域网关实现
 *
 * <p>负责 {@link FailoverEvent} 与 {@link FailoverEventDo} 的互转。
 * 审计字段（createdAt/updatedAt/createdBy/updatedBy）由
 * {@link com.codingas.gateway.infrastructure.common.BaseDo} 的
 * AuditingEntityListener 自动填充，转换时仅需透传。</p>
 *
 * <p>errorType / decision 字段以字符串存储于 DO（枚举名），读取时还原为
 * {@link ProviderErrorType} / {@link FailoverDecision} 枚举，写入时取枚举名转字符串
 * （参照 ClusterGatewayImpl 的 healthStatus 字段处理模式）。</p>
 *
 * <p>查询委派 Repository：{@code int limit} 转为 {@link Pageable}（{@code PageRequest.of(0, limit)}），
 * 排序由 Repository @Query 的 ORDER BY e.occurredAt DESC 保证。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailoverEventGatewayImpl implements FailoverEventGateway {

    private final FailoverEventRepository repository;

    @Override
    public FailoverEvent save(FailoverEvent event) {
        FailoverEventDo dataObject = toDataObject(event);
        FailoverEventDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public List<FailoverEvent> findRecent(Instant since, Long applicationId, Long clusterId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findRecent(since, applicationId, clusterId, pageable).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<FailoverEvent> findExhausted(Instant since, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findExhausted(since, pageable).stream()
                .map(this::toEntity)
                .toList();
    }

    private FailoverEvent toEntity(FailoverEventDo d) {
        FailoverEvent entity = new FailoverEvent();
        entity.setId(d.getId());
        entity.setTraceId(d.getTraceId());
        entity.setApplicationId(d.getApplicationId());
        entity.setFromChannelId(d.getFromChannelId());
        entity.setFromEndpointId(d.getFromEndpointId());
        entity.setToChannelId(d.getToChannelId());
        entity.setToEndpointId(d.getToEndpointId());
        entity.setFromClusterId(d.getFromClusterId());
        entity.setToClusterId(d.getToClusterId());
        // 枚举以字符串存储，读取时还原
        entity.setErrorType(d.getErrorType() != null ? ProviderErrorType.valueOf(d.getErrorType()) : null);
        entity.setDecision(d.getDecision() != null ? FailoverDecision.valueOf(d.getDecision()) : null);
        entity.setExhausted(d.isExhausted());
        entity.setOccurredAt(d.getOccurredAt());
        entity.setCreatedBy(d.getCreatedBy());
        entity.setCreatedAt(d.getCreatedAt());
        entity.setUpdatedBy(d.getUpdatedBy());
        entity.setUpdatedAt(d.getUpdatedAt());
        return entity;
    }

    private FailoverEventDo toDataObject(FailoverEvent entity) {
        FailoverEventDo d = new FailoverEventDo();
        d.setId(entity.getId());
        d.setTraceId(entity.getTraceId());
        d.setApplicationId(entity.getApplicationId());
        d.setFromChannelId(entity.getFromChannelId());
        d.setFromEndpointId(entity.getFromEndpointId());
        d.setToChannelId(entity.getToChannelId());
        d.setToEndpointId(entity.getToEndpointId());
        d.setFromClusterId(entity.getFromClusterId());
        d.setToClusterId(entity.getToClusterId());
        // 枚举转字符串存储
        d.setErrorType(entity.getErrorType() != null ? entity.getErrorType().name() : null);
        d.setDecision(entity.getDecision() != null ? entity.getDecision().name() : null);
        d.setExhausted(entity.isExhausted());
        d.setOccurredAt(entity.getOccurredAt());
        d.setCreatedBy(entity.getCreatedBy());
        d.setUpdatedBy(entity.getUpdatedBy());
        return d;
    }
}
