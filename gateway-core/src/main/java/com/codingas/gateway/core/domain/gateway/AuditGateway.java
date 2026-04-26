package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.AuditLog;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作审计日志。</p>
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
     * 根据团队 ID 和日期范围查询审计日志
     *
     * @param teamId 团队 ID
     * @param start 开始时间
     * @param end 结束时间
     * @return 审计日志列表
     */
    List<AuditLog> findByTeamId(Long teamId, Instant start, Instant end);

    /**
     * 根据追踪 ID 查找审计日志
     *
     * @param traceId 追踪 ID
     * @return 审计日志列表
     */
    List<AuditLog> findByTraceId(String traceId);

    /**
     * 分页查询审计日志
     *
     * @param page 页码（从 1 开始）
     * @param size 每页大小
     * @return 审计日志列表
     */
    List<AuditLog> findByPage(int page, int size);
}
