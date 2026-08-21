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
