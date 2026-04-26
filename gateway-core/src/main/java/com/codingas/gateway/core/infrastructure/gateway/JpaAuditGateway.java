package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.AuditLog;
import com.codingas.gateway.core.domain.gateway.AuditGateway;
import com.codingas.gateway.core.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志网关实现
 *
 * <p>实现 AuditGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaAuditGateway implements AuditGateway {

    private final AuditLogRepository repository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(auditLog);
    }

    @Override
    public List<AuditLog> findByTeamId(Long teamId, Instant start, Instant end) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 1000);
        return repository.findByCreatedAtBetween(start, end, pageable).getContent();
    }

    @Override
    public List<AuditLog> findByTraceId(String traceId) {
        return repository.findByTraceId(traceId)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<AuditLog> findByPage(int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        return repository.findAll(pageable).getContent();
    }
}
