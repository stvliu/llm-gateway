package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.role.RoleCreateRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleQueryRequest;
import com.codingas.gateway.adapter.admin.dto.role.RoleResponse;
import com.codingas.gateway.adapter.admin.dto.role.RoleUpdateRequest;
import com.codingas.gateway.application.role.RoleApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 *
 * <p>提供角色 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplication roleApplication;

    /**
     * 创建角色
     */
    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleApplication.create(request));
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(roleApplication.getById(id));
    }

    /**
     * 查询角色列表
     */
    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> query(@ModelAttribute RoleQueryRequest request) {
        return ApiResponse.success(roleApplication.query(request));
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleApplication.update(id, request));
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleApplication.delete(id);
        return ApiResponse.success();
    }

    /**
     * 启用/禁用角色
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<RoleResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.success(roleApplication.setEnabled(id, enabled));
    }
}
