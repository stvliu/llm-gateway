package com.codingas.gateway.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest {
    @Size(min = 2, max = 64, message = "用户名长度必须在 2-64 之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String avatarUrl;
}
