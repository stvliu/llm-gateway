package com.codingas.gateway.application.dto.user;

import com.codingas.gateway.common.enums.UserStatus;
import lombok.Data;
import java.time.Instant;

/**
 * 用户响应
 */
@Data
public class UserResponse {
    private Long id;
    private String userCode;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private Boolean emailVerified;
    /**
     * 用户角色：ADMIN（管理员）/ USER（普通用户）
     */
    private String role;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
