/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.dto.ModelCreateRequest;
import com.codingas.gateway.application.model.dto.ModelQueryRequest;
import com.codingas.gateway.application.model.dto.ModelResponse;
import com.codingas.gateway.application.model.dto.ModelUpdateRequest;
import com.codingas.gateway.application.model.ModelService;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模型管理控制器
 *
 * <p>提供模型 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    /**
     * 创建模型
     */
    @PostMapping
    public ModelResponse create(@Valid @RequestBody ModelCreateRequest request) {
        return modelService.create(request);
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{id}")
    public ModelResponse getById(@PathVariable Long id) {
        return modelService.getById(id);
    }

    /**
     * 查询模型列表
     */
    @GetMapping
    public PageResponse<ModelResponse> query(@ModelAttribute ModelQueryRequest request) {
        return modelService.query(request);
    }

    /**
     * 更新模型
     */
    @PutMapping("/{id}")
    public ModelResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ModelUpdateRequest request) {
        return modelService.update(id, request);
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        modelService.delete(id);
    }

    /**
     * 启用/禁用模型
     */
    @PatchMapping("/{id}/state")
    public ModelResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return modelService.setEnabled(id, enabled);
    }
}
