package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.application.user.UserService;
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
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 查询用户列表
     */
    @GetMapping
    public PageResponse<UserResponse> query(@ModelAttribute UserQueryRequest request) {
        return userService.query(request);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/state")
    public UserResponse updateState(
            @PathVariable Long id,
            @Valid @RequestBody UserStateUpdateRequest request) {
        return userService.updateState(id, request);
    }

    /**
     * 分配用户角色
     */
    @PutMapping("/{id}/roles")
    public UserResponse assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignRequest request) {
        return userService.assignRoles(id, request);
    }
}