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

import com.codingas.gateway.usage.tokenlimit.TokenLimitManager;
import com.codingas.gateway.web.api.dto.TokenLimitCreateRequest;
import com.codingas.gateway.web.api.dto.TokenLimitQueryRequest;
import com.codingas.gateway.web.api.dto.TokenLimitResponse;
import com.codingas.gateway.web.api.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Token 限额管理控制器
 *
 * <p>提供 Token 限额 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/token-limits")
@RequiredArgsConstructor
public class TokenLimitController {

    private final TokenLimitManager tokenLimitManager;

    /**
     * 创建 Token 限额
     */
    @PostMapping
    public TokenLimitResponse create(@Valid @RequestBody TokenLimitCreateRequest request) {
        return TokenLimitResponse.from(tokenLimitManager.create(
                request.getUserId(), request.getProviderId(), request.getModelId(),
                request.getSwitchModelId(), request.toEntity()));
    }

    /**
     * 获取 Token 限额详情
     */
    @GetMapping("/{id}")
    public TokenLimitResponse getById(@PathVariable Long id) {
        return TokenLimitResponse.from(tokenLimitManager.getById(id));
    }

    /**
     * 查询 Token 限额列表
     */
    @GetMapping
    public PageResponse<TokenLimitResponse> query(@ModelAttribute TokenLimitQueryRequest request) {
        return TokenLimitResponse.fromPage(tokenLimitManager.query(request.toQuery()));
    }

    /**
     * 更新 Token 限额
     */
    @PutMapping("/{id}")
    public TokenLimitResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TokenLimitUpdateRequest request) {
        return TokenLimitResponse.from(tokenLimitManager.update(id, request.toEntity(), request.getSwitchModelId()));
    }

    /**
     * 删除 Token 限额
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tokenLimitManager.delete(id);
    }

    /**
     * 重置已使用量
     */
    @PatchMapping("/{id}/reset-usage")
    public TokenLimitResponse resetUsage(@PathVariable Long id) {
        return TokenLimitResponse.from(tokenLimitManager.resetUsage(id));
    }
}
