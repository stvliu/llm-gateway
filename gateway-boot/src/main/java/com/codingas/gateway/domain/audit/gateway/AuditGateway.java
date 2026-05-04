package com.codingas.gateway.domain.audit.gateway;

import com.codingas.gateway.domain.audit.entity.AuditLog;

import java.util.List;

/**
 * 审计日志网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface AuditGateway {

    /**
     * 保存审计日志
     *
     * @param auditLog 审计日志实体
     * @return 保存后的实体
     */
    AuditLog save(AuditLog auditLog);

    /**
     * 根据用户 ID 查找审计日志
     *
     * @param userId 用户 ID
     * @return 审计日志列表
     */
    List<AuditLog> findByUserId(Long userId);
}
