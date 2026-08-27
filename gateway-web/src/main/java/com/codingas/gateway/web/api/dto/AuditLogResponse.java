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

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.common.dto.PageResponse;
import lombok.Data;

import java.time.Instant;

/**
 * 审计日志响应 DTO（HTTP 契约）
 */
@Data
public class AuditLogResponse {

    private Long id;

    /** 操作人用户 ID */
    private Long userId;

    /** 操作动作（如 "POST /api/v1/channels"） */
    private String action;

    /** 操作资源路径 */
    private String resource;

    /** 操作结果（SUCCESS/FAILURE） */
    private String result;

    /** 客户端 IP */
    private String ipAddress;

    /** 操作时间 */
    private Instant createdAt;

    /**
     * 从审计日志实体转换
     *
     * @param log 审计日志实体
     * @return 响应 DTO
     */
    public static AuditLogResponse from(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setAction(log.getAction());
        response.setResource(log.getResource());
        response.setResult(log.getResult());
        response.setIpAddress(log.getIpAddress());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    /**
     * 从审计日志实体分页转换
     *
     * @param page 审计日志分页
     * @return 响应 DTO 分页
     */
    public static PageResponse<AuditLogResponse> fromPage(PageResponse<AuditLog> page) {
        return PageResponse.of(
                page.getItems().stream().map(AuditLogResponse::from).toList(),
                page.getPagination().getPage(),
                page.getPagination().getLimit(),
                page.getPagination().getTotal());
    }
}
