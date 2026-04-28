package com.codingas.gateway.adapter.admin.dto.role;

import com.codingas.gateway.domain.security.entity.Role.RoleType;
import lombok.Data;

import java.time.Instant;

/**
 * 角色响应
 */
@Data
public class RoleResponse {

    private Long id;
    private String roleCode;
    private String name;
    private String description;
    private RoleType roleType;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
