package com.codingas.gateway.infrastructure.resilience.gateway.database.repository;

import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.FailoverEventDo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件 JPA Repository
 *
 * <p>提供转移事件的持久化与查询能力。查询方法均按 occurredAt 倒序返回。</p>
 */
public interface FailoverEventRepository extends JpaRepository<FailoverEventDo, Long> {

    /**
     * 查询近期转移事件（按 occurredAt 倒序）
     *
     * <p>since/applicationId/clusterId 均可选：为 null 时不参与过滤。
     * clusterId 匹配冗余 fromClusterId 或 toClusterId（任一命中即返回）。</p>
     *
     * @param since         起始时间（可空）
     * @param applicationId 应用 ID（可空）
     * @param clusterId     故障域 ID（可空）
     * @param pageable      分页（取 limit 条）
     * @return 转移事件 DO 列表（按 occurredAt 倒序）
     */
    @Query("SELECT e FROM FailoverEventDo e " +
            "WHERE (:since IS NULL OR e.occurredAt >= :since) " +
            "AND (:applicationId IS NULL OR e.applicationId = :applicationId) " +
            "AND (:clusterId IS NULL OR e.fromClusterId = :clusterId OR e.toClusterId = :clusterId) " +
            "ORDER BY e.occurredAt DESC")
    List<FailoverEventDo> findRecent(@Param("since") Instant since,
                                     @Param("applicationId") Long applicationId,
                                     @Param("clusterId") Long clusterId,
                                     Pageable pageable);

    /**
     * 查询耗尽告警事件（exhausted=true，按 occurredAt 倒序）
     *
     * @param since    起始时间（可空）
     * @param pageable 分页（取 limit 条）
     * @return 耗尽事件 DO 列表（按 occurredAt 倒序）
     */
    @Query("SELECT e FROM FailoverEventDo e " +
            "WHERE e.exhausted = true " +
            "AND (:since IS NULL OR e.occurredAt >= :since) " +
            "ORDER BY e.occurredAt DESC")
    List<FailoverEventDo> findExhausted(@Param("since") Instant since,
                                        Pageable pageable);
}
