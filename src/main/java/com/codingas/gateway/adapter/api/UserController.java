package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.application.user.UserService;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * <p>提供用户 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /**
     * 查询用户列表
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> query(@ModelAttribute UserQueryRequest request) {
        return ApiResponse.success(userService.query(request));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success(userService.updateStatus(id, request));
    }

    /**
     * 分配用户角色
     */
    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignRequest request) {
        return ApiResponse.success(userService.assignRoles(id, request));
    }
}
