package com.codingas.gateway.adapter.admin.dto.role;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新角色请求
 */
@Data
public class RoleUpdateRequest {

    @Size(max = 64, message = "Role name must not exceed 64 characters")
    private String name;

    private String description;

    private Boolean isActive;
}
