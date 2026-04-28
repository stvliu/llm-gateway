package com.codingas.gateway.adapter.admin.dto.role;

import com.codingas.gateway.domain.security.entity.Role.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建角色请求
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "Role code is required")
    @Size(max = 64, message = "Role code must not exceed 64 characters")
    private String roleCode;

    @NotBlank(message = "Role name is required")
    @Size(max = 64, message = "Role name must not exceed 64 characters")
    private String name;

    private String description;

    private RoleType roleType = RoleType.CUSTOM;
}
