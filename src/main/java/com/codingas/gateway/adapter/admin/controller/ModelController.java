package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.model.ModelCreateRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelQueryRequest;
import com.codingas.gateway.adapter.admin.dto.model.ModelResponse;
import com.codingas.gateway.adapter.admin.dto.model.ModelUpdateRequest;
import com.codingas.gateway.application.model.ModelApplication;
import com.codingas.gateway.common.dto.ApiResponse;
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

    private final ModelApplication modelApplication;

    /**
     * 创建模型
     */
    @PostMapping
    public ApiResponse<ModelResponse> create(@Valid @RequestBody ModelCreateRequest request) {
        return ApiResponse.success(modelApplication.create(request));
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ModelResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(modelApplication.getById(id));
    }

    /**
     * 查询模型列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ModelResponse>> query(@ModelAttribute ModelQueryRequest request) {
        return ApiResponse.success(modelApplication.query(request));
    }

    /**
     * 更新模型
     */
    @PutMapping("/{id}")
    public ApiResponse<ModelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ModelUpdateRequest request) {
        return ApiResponse.success(modelApplication.update(id, request));
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelApplication.delete(id);
        return ApiResponse.success();
    }

    /**
     * 启用/禁用模型
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<ModelResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.success(modelApplication.setEnabled(id, enabled));
    }
}
