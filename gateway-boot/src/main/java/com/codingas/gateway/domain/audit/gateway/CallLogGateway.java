/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.audit.gateway;

import com.codingas.gateway.domain.audit.entity.CallLog;

import java.util.List;
import java.util.Optional;

/**
 * 调用日志网关接口
 */
public interface CallLogGateway {

    /**
     * 保存调用日志
     */
    CallLog save(CallLog callLog);

    /**
     * 根据追踪 ID 查找调用日志
     */
    Optional<CallLog> findByTraceId(String traceId);

    /**
     * 根据用户 ID 查找调用日志
     */
    List<CallLog> findByUserId(Long userId);
}
