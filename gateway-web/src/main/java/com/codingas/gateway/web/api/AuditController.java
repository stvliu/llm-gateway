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
package com.codingas.gateway.web.api;

import com.codingas.gateway.audit.AuditService;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.web.api.dto.AuditLogQueryRequest;
import com.codingas.gateway.web.api.dto.AuditLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 审计日志控制器
 *
 * <p>提供管理操作审计日志的分页查询端点。
 * 权限由 {@code PermissionInterceptor} 默认保证：{@code /api/v1/audit-logs}
 * 不在 USER 白名单，仅 ADMIN 可访问。</p>
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * 分页查询审计日志
     *
     * @param request 查询条件（分页 + 筛选）
     * @return 分页结果
     */
    @GetMapping
    public PageResponse<AuditLogResponse> query(@Valid @ModelAttribute AuditLogQueryRequest request) {
        return AuditLogResponse.fromPage(auditService.query(request.toQuery()));
    }

    /**
     * 手动清理审计日志
     *
     * <p>按 {@code days}（保留 N 天前）或 {@code before}（ISO-8601 时间点）计算截止时间，
     * 删除 createdAt 早于截止时间的审计日志；两者至少提供一个，同时提供时以 {@code days} 为准。
     * 返回删除条数（生产环境由 ApiResponseWrapperAdvice 包装为 {@code {data: {deleted: N}}}）。</p>
     *
     * @param days   保留天数（可选，N 天前的日志将被删除）
     * @param before 截止时间（可选，ISO-8601 格式）
     * @return 包含删除条数 {@code deleted} 的映射
     * @throws IllegalArgumentException days 与 before 均未提供时抛出
     */
    @DeleteMapping
    public Map<String, Integer> delete(@RequestParam(required = false) Integer days,
                                       @RequestParam(required = false) Instant before) {
        Instant cutoff;
        if (days != null) {
            cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        } else if (before != null) {
            cutoff = before;
        } else {
            throw new IllegalArgumentException("days 与 before 至少提供一个");
        }
        int deleted = auditService.deleteBefore(cutoff);
        return Map.of("deleted", deleted);
    }
}
