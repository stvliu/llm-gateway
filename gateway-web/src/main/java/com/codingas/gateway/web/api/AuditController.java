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

import com.codingas.gateway.audit.AuditManager;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.web.api.dto.AuditLogQueryRequest;
import com.codingas.gateway.web.api.dto.AuditLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final AuditManager auditManager;

    /**
     * 分页查询审计日志
     *
     * @param request 查询条件（分页 + 筛选）
     * @return 分页结果
     */
    @GetMapping
    public PageResponse<AuditLogResponse> query(@Valid @ModelAttribute AuditLogQueryRequest request) {
        return AuditLogResponse.fromPage(auditManager.query(request.toQuery()));
    }
}
