package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.auth.dto.ChangePasswordRequest;
import com.codingas.gateway.application.auth.dto.LoginRequest;
import com.codingas.gateway.application.auth.dto.LoginResponse;
import com.codingas.gateway.application.user.UserService;
import com.codingas.gateway.application.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供用户登录、登出、获取当前用户信息等认证相关的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public void logout() {
        // JWT 无状态认证，服务端无需处理登出
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        // TODO: 从 SecurityContext 获取当前用户 ID
        // 目前返回模拟数据
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("admin");
        response.setEmail("admin@example.com");
        response.setRole("ADMIN");
        return response;
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // TODO: 从 SecurityContext 获取当前用户 ID
        userService.changePassword(1L, request);
    }
}
