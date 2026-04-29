package com.codingas.gateway.adapter.admin.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 分配用户角色请求
 */
@Data
public class UserRoleAssignRequest {
    @NotEmpty(message = "必须至少分配一个角色")
    private List<String> roleCodes;
}
