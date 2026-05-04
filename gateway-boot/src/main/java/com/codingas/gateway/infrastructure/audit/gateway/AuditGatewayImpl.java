package com.codingas.gateway.infrastructure.audit.gateway;

import com.codingas.gateway.domain.audit.entity.AuditLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.infrastructure.audit.gateway.database.AuditLogRepository;
import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.AuditLogDo;
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
        AuditLogDo doEntity = toDo(auditLog);
        AuditLogDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toEntity)
                .toList();
    }

    private AuditLog toEntity(AuditLogDo doEntity) {
        if (doEntity == null) return null;
        AuditLog entity = new AuditLog();
        entity.setUserId(doEntity.getUserId());
        entity.setAction(doEntity.getAction());
        entity.setResource(doEntity.getResource());
        entity.setResult(doEntity.getResult());
        entity.setIpAddress(doEntity.getIpAddress());
        return entity;
    }

    private AuditLogDo toDo(AuditLog entity) {
        if (entity == null) return null;
        AuditLogDo doEntity = new AuditLogDo();
        doEntity.setUserId(entity.getUserId());
        doEntity.setAction(entity.getAction());
        doEntity.setResource(entity.getResource());
        doEntity.setResult(entity.getResult());
        doEntity.setIpAddress(entity.getIpAddress());
        return doEntity;
    }
}
