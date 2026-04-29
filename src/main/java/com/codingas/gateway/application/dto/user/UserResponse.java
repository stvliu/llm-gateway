package com.codingas.gateway.adapter.admin.dto.user;

import com.codingas.gateway.common.enums.UserStatus;
import lombok.Data;
import java.time.Instant;
import java.util.List;

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
    private List<RoleInfo> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class RoleInfo {
        private String roleCode;
        private String name;
    }
}
