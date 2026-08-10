/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.audit.gateway.database.repository;

import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.CallLogDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 调用日志 JPA Repository
 */
public interface CallLogRepository extends JpaRepository<CallLogDo, Long> {

    Optional<CallLogDo> findByTraceId(String traceId);

    List<CallLogDo> findByUserId(Long userId);
}
