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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.audit.AuditLogQuery;
import com.codingas.gateway.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 审计日志查询请求 DTO（HTTP 契约）
 *
 * <p>时间参数为 ISO-8601 字符串（如 {@code 2026-08-27T10:00:00Z}），由 Spring 绑定为
 * {@link Instant}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQueryRequest extends PageRequest {

    /** 操作人用户 ID */
    private Long userId;

    /** 操作动作（模糊匹配，如 "POST /api/v1/channels"） */
    private String action;

    /** 操作结果（SUCCESS/FAILURE） */
    private String result;

    /** 开始时间（含） */
    private Instant startTime;

    /** 结束时间（含） */
    private Instant endTime;

    /**
     * 转换为核心查询条件入参
     *
     * @return 查询条件
     */
    public AuditLogQuery toQuery() {
        AuditLogQuery query = new AuditLogQuery();
        query.setPage(getPage());
        query.setLimit(getLimit());
        query.setUserId(userId);
        query.setAction(action);
        query.setResult(result);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        return query;
    }
}
