package com.codingas.gateway.infrastructure.audit.gateway;

import com.codingas.gateway.domain.audit.entity.AuditLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志网关实现
 *
 * <p>实现 AuditGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuditGatewayImpl implements AuditGateway {

    private final AuditLogRepository repository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(auditLog);
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        return repository.findAll().stream()
                .filter(a -> a.getUserId() != null && a.getUserId().equals(userId))
                .toList();
    }
}

/**
 * 审计日志仓储接口
 */
interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findAll();
}
