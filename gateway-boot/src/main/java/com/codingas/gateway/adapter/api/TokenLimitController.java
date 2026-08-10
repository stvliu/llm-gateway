/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.quota.dto.TokenLimitCreateRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitQueryRequest;
import com.codingas.gateway.application.quota.dto.TokenLimitResponse;
import com.codingas.gateway.application.quota.dto.TokenLimitUpdateRequest;
import com.codingas.gateway.application.quota.TokenLimitService;
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

    private final TokenLimitService tokenLimitService;

    /**
     * 创建 Token 限额
     */
    @PostMapping
    public TokenLimitResponse create(@Valid @RequestBody TokenLimitCreateRequest request) {
        return tokenLimitService.create(request);
    }

    /**
     * 获取 Token 限额详情
     */
    @GetMapping("/{id}")
    public TokenLimitResponse getById(@PathVariable Long id) {
        return tokenLimitService.getById(id);
    }

    /**
     * 查询 Token 限额列表
     */
    @GetMapping
    public PageResponse<TokenLimitResponse> query(@ModelAttribute TokenLimitQueryRequest request) {
        return tokenLimitService.query(request);
    }

    /**
     * 更新 Token 限额
     */
    @PutMapping("/{id}")
    public TokenLimitResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TokenLimitUpdateRequest request) {
        return tokenLimitService.update(id, request);
    }

    /**
     * 删除 Token 限额
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tokenLimitService.delete(id);
    }

    /**
     * 重置已使用量
     */
    @PatchMapping("/{id}/reset-usage")
    public TokenLimitResponse resetUsage(@PathVariable Long id) {
        return tokenLimitService.resetUsage(id);
    }
}