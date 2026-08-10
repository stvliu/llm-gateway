/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.audit.gateway.database;

import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.AuditLogDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志 Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogDo, Long> {
    List<AuditLogDo> findByUserId(Long userId);
}
