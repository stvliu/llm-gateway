/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.audit.gateway.database;

import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.UsageLogDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * UsageLog JPA Repository
 *
 * <p>提供用量统计的数据访问能力。</p>
 */
@Repository
public interface UsageLogRepository extends JpaRepository<UsageLogDo, Long> {

    /**
     * 按 API Key 批量聚合统计
     *
     * @param apiKeyIds API Key ID 列表
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 聚合结果数组 [apiKeyId, totalCalls, totalInputTokens, totalOutputTokens, totalTokens]
     */
    @Query("""
        SELECT ul.userApiKeyId AS apiKeyId,
               COUNT(ul) AS totalCalls,
               COALESCE(SUM(ul.inputTokens), 0) AS totalInputTokens,
               COALESCE(SUM(ul.outputTokens), 0) AS totalOutputTokens,
               COALESCE(SUM(ul.totalTokens), 0) AS totalTokens
        FROM UsageLogDo ul
        WHERE ul.userApiKeyId IN :apiKeyIds
          AND ul.createdAt >= :startDate
          AND ul.createdAt < :endDate
        GROUP BY ul.userApiKeyId
    """)
    List<Object[]> aggregateByApiKeyIds(
        @Param("apiKeyIds") List<Long> apiKeyIds,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * 单个 API Key 聚合统计
     *
     * @param apiKeyId  API Key ID
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 聚合结果数组 [totalCalls, totalInputTokens, totalOutputTokens, totalTokens]
     */
    @Query("""
        SELECT COUNT(ul) AS totalCalls,
               COALESCE(SUM(ul.inputTokens), 0) AS totalInputTokens,
               COALESCE(SUM(ul.outputTokens), 0) AS totalOutputTokens,
               COALESCE(SUM(ul.totalTokens), 0) AS totalTokens
        FROM UsageLogDo ul
        WHERE ul.userApiKeyId = :apiKeyId
          AND ul.createdAt >= :startDate
          AND ul.createdAt < :endDate
    """)
    List<Object[]> aggregateByApiKeyId(
        @Param("apiKeyId") Long apiKeyId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}
