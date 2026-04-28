package com.codingas.gateway.adapter.admin.dto.role;

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.domain.security.entity.Role.RoleType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询角色请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryRequest extends PageRequest {

    private String keyword;

    private RoleType roleType;

    private Boolean isActive;
}
