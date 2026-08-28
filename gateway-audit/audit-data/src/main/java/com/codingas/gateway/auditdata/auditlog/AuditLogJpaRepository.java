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
package com.codingas.gateway.auditdata.auditlog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 审计日志 JPA Repository
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogDo, Long> {

    /**
     * 批量删除创建时间早于截止时间的审计日志
     *
     * <p>JPQL 引用基类 {@link com.codingas.gateway.common.data.BaseDo} 的 createdAt 字段，
     * 对应数据库 audit_logs.created_at 列；自含事务，供无外层事务的定时任务直接调用。</p>
     *
     * @param cutoff 截止时间（不含），createdAt 早于该时间的日志将被删除
     * @return 删除的条数
     */
    @Transactional
    // clearAutomatically：批量删除后清空持久化上下文，避免同事务后续读取到旧缓存实体
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AuditLogDo a WHERE a.createdAt < :cutoff")
    int deleteBefore(@Param("cutoff") Instant cutoff);
}
